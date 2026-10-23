package com.facevoice.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallActivity extends Activity {

    private static final int PERMISSION_REQUEST = 200;
    private static final String TAG = "FaceVoiceCall";

    private LinearLayout root;
    private EditText roomInput;
    private TextView statusText;
    private Button createButton;
    private Button joinButton;

    private SurfaceViewRenderer localRenderer;
    private SurfaceViewRenderer remoteRenderer;

    private EglBase eglBase;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private VideoCapturer cameraCapturer;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private FirebaseAuth auth;
    private DatabaseReference database;
    private DatabaseReference currentCall;
    private ValueEventListener answerListener;
    private ChildEventListener candidateListener;

    private boolean isCaller = false;
    private boolean callStarted = false;
    private boolean shuttingDown = false;

    private final List<IceCandidate> pendingCandidates =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildScreen();

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                    },
                    PERMISSION_REQUEST
            );
        } else {
            signInAndInitialize();
        }
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);
        root.setBackgroundColor(Color.rgb(18, 22, 32));

        scroll.addView(root);
        setContentView(scroll);

        TextView heading = new TextView(this);
        heading.setText("Face & Voice Studio — Video Call");
        heading.setTextSize(22);
        heading.setTextColor(Color.WHITE);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(0, 8, 0, 16);
        root.addView(heading);

        statusText = new TextView(this);
        statusText.setText("Preparing camera and microphone...");
        statusText.setTextColor(Color.LTGRAY);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 8, 0, 12);
        root.addView(statusText);

        localRenderer = new SurfaceViewRenderer(this);
        remoteRenderer = new SurfaceViewRenderer(this);

        root.addView(localRenderer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 360));

        root.addView(remoteRenderer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 360));

        TextView roomLabel = new TextView(this);
        roomLabel.setText("Room ID — share this with the other person");
        roomLabel.setTextColor(Color.WHITE);
        roomLabel.setPadding(0, 14, 0, 6);
        root.addView(roomLabel);

        roomInput = new EditText(this);
        roomInput.setSingleLine(true);
        roomInput.setHint("Enter room ID to join a call");
        roomInput.setHintTextColor(Color.GRAY);
        roomInput.setTextColor(Color.BLACK);
        roomInput.setBackgroundColor(Color.WHITE);
        root.addView(roomInput);

        createButton = makeButton("Create New Call");
        createButton.setOnClickListener(v -> createCall());
        root.addView(createButton);

        joinButton = makeButton("Join Call Using Room ID");
        joinButton.setOnClickListener(v -> joinCall());
        root.addView(joinButton);

        Button copyHint = makeButton("How to Connect");
        copyHint.setOnClickListener(v ->
                showMessage("One person creates a call and shares the room ID. "
                        + "The other person enters that ID and taps Join Call."));
        root.addView(copyHint);

        Button endButton = makeButton("End Call");
        endButton.setOnClickListener(v -> endCall());
        root.addView(endButton);
    }

    private Button makeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 6, 0, 6);
        button.setLayoutParams(params);
        return button;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST) {
            boolean granted = grantResults.length >= 2;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                signInAndInitialize();
            } else {
                updateStatus("Camera and microphone permissions are required.");
            }
        }
    }

    private void signInAndInitialize() {
        updateStatus("Signing in securely...");

        if (auth.getCurrentUser() != null) {
            initializeWebRTC();
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result -> initializeWebRTC())
                .addOnFailureListener(error ->
                        updateStatus("Firebase sign-in failed: "
                                + error.getMessage()));
    }

    private void initializeWebRTC() {
        try {
            updateStatus("Starting camera and audio...");

            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions
                            .builder(this)
                            .createInitializationOptions());

            eglBase = EglBase.create();

            localRenderer.init(eglBase.getEglBaseContext(), null);
            localRenderer.setMirror(true);
            localRenderer.setEnableHardwareScaler(true);

            remoteRenderer.init(eglBase.getEglBaseContext(), null);
            remoteRenderer.setMirror(false);
            remoteRenderer.setEnableHardwareScaler(true);

            PeerConnectionFactory.Options options =
                    new PeerConnectionFactory.Options();

            factory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .setAudioDeviceModule(
                            JavaAudioDeviceModule.builder(this).createAudioDeviceModule())
                    .setVideoEncoderFactory(
                            new DefaultVideoEncoderFactory(
                                    eglBase.getEglBaseContext(),
                                    true,
                                    true))
                    .setVideoDecoderFactory(
                            new DefaultVideoDecoderFactory(
                                    eglBase.getEglBaseContext()))
                    .createPeerConnectionFactory();

            startLocalMedia();

            updateStatus("Ready. Create a call or enter a room ID.");
            createButton.setEnabled(true);
            joinButton.setEnabled(true);

        } catch (Exception error) {
            updateStatus("WebRTC setup failed: " + error.getMessage());
        }
    }

    private void startLocalMedia() {
        Camera2Enumerator enumerator = new Camera2Enumerator(this);
        String[] deviceNames = enumerator.getDeviceNames();

        for (String name : deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                cameraCapturer = enumerator.createCapturer(name, null);
                if (cameraCapturer != null) {
                    break;
                }
            }
        }

        if (cameraCapturer == null) {
            for (String name : deviceNames) {
                if (enumerator.isBackFacing(name)) {
                    cameraCapturer = enumerator.createCapturer(name, null);
                    if (cameraCapturer != null) {
                        break;
                    }
                }
            }
        }

        if (cameraCapturer != null) {
            videoSource = factory.createVideoSource(false);
            cameraCapturer.initialize(
                    org.webrtc.SurfaceTextureHelper.create(
                            "CameraCaptureThread",
                            eglBase.getEglBaseContext()),
                    this,
                    videoSource.getCapturerObserver());

            try {
                cameraCapturer.startCapture(640, 480, 24);
            } catch (Exception error) {
                updateStatus("Could not start camera: " + error.getMessage());
            }

            localVideoTrack =
                    factory.createVideoTrack("local_video", videoSource);
            localVideoTrack.addSink(localRenderer);
        }

        audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("local_audio", audioSource);
        localAudioTrack.setEnabled(true);
    }

    private PeerConnection createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = Arrays.asList(
                PeerConnection.IceServer.builder(
                        "stun:stun.l.google.com:19302").createIceServer()
        );

        PeerConnection.RTCConfiguration configuration =
                new PeerConnection.RTCConfiguration(iceServers);

        configuration.sdpSemantics =
                PeerConnection.SdpSemantics.UNIFIED_PLAN;

        PeerConnection.Observer observer = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(
                    PeerConnection.SignalingState state) {}

            @Override
            public void onIceConnectionChange(
                    PeerConnection.IceConnectionState state) {
                runOnUiThread(() -> updateStatus("Call connection: " + state));
            }

            @Override
            public void onIceConnectionReceivingChange(boolean receiving) {}

            @Override
            public void onIceGatheringChange(
                    PeerConnection.IceGatheringState state) {}

            @Override
            public void onIceCandidate(IceCandidate candidate) {
                sendIceCandidate(candidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] candidates) {}

            @Override
            public void onAddStream(MediaStream stream) {
                if (stream.videoTracks.size() > 0) {
                    VideoTrack track = stream.videoTracks.get(0);
                    runOnUiThread(() -> track.addSink(remoteRenderer));
                }
            }

            @Override
            public void onRemoveStream(MediaStream stream) {}

            @Override
            public void onDataChannel(DataChannel channel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(
                    RtpReceiver receiver, MediaStream[] streams) {
                if (receiver.track() instanceof VideoTrack) {
                    VideoTrack track = (VideoTrack) receiver.track();
                    runOnUiThread(() -> track.addSink(remoteRenderer));
                }
            }

            @Override
            public void onTrack(RtpTransceiver transceiver) {
                if (transceiver.getReceiver().track() instanceof VideoTrack) {
                    VideoTrack track =
                            (VideoTrack) transceiver.getReceiver().track();
                    runOnUiThread(() -> track.addSink(remoteRenderer));
                }
            }
        };

        PeerConnection connection =
                factory.createPeerConnection(configuration, observer);

        if (connection != null) {
            if (localAudioTrack != null) {
                connection.addTrack(localAudioTrack);
            }
            if (localVideoTrack != null) {
                connection.addTrack(localVideoTrack);
            }
        }

        return connection;
    }

    private void createCall() {
        if (factory == null || auth.getCurrentUser() == null) {
            showMessage("Please wait until setup finishes.");
            return;
        }

        String roomId = database.child("calls").push().getKey();
        if (roomId == null) {
            updateStatus("Could not create a room ID.");
            return;
        }

        isCaller = true;
        callStarted = true;
        currentCall = database.child("calls").child(roomId);
        roomInput.setText(roomId);
        roomInput.setSelectAllOnFocus(true);

        updateStatus("Creating call room...");

        Map<String, Object> roomData = new HashMap<>();
        roomData.put("callerUid", auth.getCurrentUser().getUid());

        currentCall.updateChildren(roomData)
                .addOnSuccessListener(unused -> {
                    peerConnection = createPeerConnection();

                    if (peerConnection == null) {
                        updateStatus("Could not create WebRTC connection.");
                        return;
                    }

                    peerConnection.createOffer(new SdpObserver() {
                        @Override
                        public void onCreateSuccess(SessionDescription sdp) {
                            peerConnection.setLocalDescription(
                                    new SimpleSdpObserver(), sdp);

                            Map<String, Object> offer = new HashMap<>();
                            offer.put("type", sdp.type.canonicalForm());
                            offer.put("sdp", sdp.description);

                            currentCall.child("offer").setValue(offer)
                                    .addOnSuccessListener(done -> {
                                        updateStatus("Call created. Share the room ID.");
                                        listenForAnswer();
                                        listenForCandidates("calleeCandidates");
                                    })
                                    .addOnFailureListener(error ->
                                            updateStatus("Could not publish offer: "
                                                    + error.getMessage()));
                        }

                        @Override
                        public void onSetSuccess() {}

                        @Override
                        public void onCreateFailure(String error) {
                            updateStatus("Offer failed: " + error);
                        }

                        @Override
                        public void onSetFailure(String error) {}
                    }, new MediaConstraints());
                })
                .addOnFailureListener(error ->
                        updateStatus("Room creation failed: "
                                + error.getMessage()));
    }

    private void joinCall() {
        if (factory == null || auth.getCurrentUser() == null) {
            showMessage("Please wait until setup finishes.");
            return;
        }

        String roomId = roomInput.getText().toString().trim();

        if (roomId.isEmpty()) {
            showMessage("Enter the room ID shared by the caller.");
            return;
        }

        isCaller = false;
        callStarted = true;
        currentCall = database.child("calls").child(roomId);

        updateStatus("Joining call room...");

        currentCall.child("offer").get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        updateStatus("No offer found. Check the room ID.");
                        return;
                    }

                    currentCall.child("calleeUid")
                            .setValue(auth.getCurrentUser().getUid())
                            .addOnSuccessListener(unused -> {
                                peerConnection = createPeerConnection();

                                if (peerConnection == null) {
                                    updateStatus("Could not create WebRTC connection.");
                                    return;
                                }

                                String type = snapshot.child("type")
                                        .getValue(String.class);
                                String sdpText = snapshot.child("sdp")
                                        .getValue(String.class);

                                if (type == null || sdpText == null) {
                                    updateStatus("The offer data is incomplete.");
                                    return;
                                }

                                SessionDescription offer =
                                        new SessionDescription(
                                                SessionDescription.Type.fromCanonicalForm(type),
                                                sdpText);

                                peerConnection.setRemoteDescription(
                                        new SimpleSdpObserver() {
                                            @Override
                                            public void onSetSuccess() {
                                                createAnswer();
                                            }
                                        }, offer);

                                listenForCandidates("callerCandidates");
                            })
                            .addOnFailureListener(error ->
                                    updateStatus("Could not join room: "
                                            + error.getMessage()));
                })
                .addOnFailureListener(error ->
                        updateStatus("Could not read room: "
                                + error.getMessage()));
    }

    private void createAnswer() {
        peerConnection.createAnswer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                peerConnection.setLocalDescription(
                        new SimpleSdpObserver(), sdp);

                Map<String, Object> answer = new HashMap<>();
                answer.put("type", sdp.type.canonicalForm());
                answer.put("sdp", sdp.description);

                currentCall.child("answer").setValue(answer)
                        .addOnSuccessListener(unused ->
                                updateStatus("Answer sent. Connecting..."))
                        .addOnFailureListener(error ->
                                updateStatus("Could not send answer: "
                                        + error.getMessage()));
            }

            @Override
            public void onSetSuccess() {}

            @Override
            public void onCreateFailure(String error) {
                updateStatus("Answer creation failed: " + error);
            }

            @Override
            public void onSetFailure(String error) {}
        }, new MediaConstraints());
    }

    private void listenForAnswer() {
        answerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists() || peerConnection == null) {
                    return;
                }

                String type = snapshot.child("type").getValue(String.class);
                String sdpText = snapshot.child("sdp").getValue(String.class);

                if (type == null || sdpText == null) {
                    return;
                }

                SessionDescription answer = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type),
                        sdpText);

                peerConnection.setRemoteDescription(
                        new SimpleSdpObserver(), answer);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                updateStatus("Answer listener failed: " + error.getMessage());
            }
        };

        currentCall.child("answer").addValueEventListener(answerListener);
    }

    private void listenForCandidates(String childName) {
        candidateListener = new ChildEventListener() {
            @Override
            public void onChildAdded(
                    DataSnapshot snapshot, String previousChildName) {
                String sdp = snapshot.child("sdp").getValue(String.class);
                String mid = snapshot.child("sdpMid").getValue(String.class);
                Object indexValue = snapshot.child("sdpMLineIndex").getValue();

                if (sdp == null || indexValue == null
                        || peerConnection == null) {
                    return;
                }

                int index = ((Number) indexValue).intValue();

                peerConnection.addIceCandidate(
                        new IceCandidate(mid, index, sdp));
            }

            @Override
            public void onChildChanged(
                    DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(
                    DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                updateStatus("ICE listener failed: " + error.getMessage());
            }
        };

        currentCall.child(childName).addChildEventListener(candidateListener);
    }

    private void sendIceCandidate(IceCandidate candidate) {
        if (currentCall == null) {
            return;
        }

        String childName = isCaller
                ? "callerCandidates"
                : "calleeCandidates";

        Map<String, Object> data = new HashMap<>();
        data.put("sdpMid", candidate.sdpMid);
        data.put("sdpMLineIndex", candidate.sdpMLineIndex);
        data.put("sdp", candidate.sdp);

        currentCall.child(childName).push().setValue(data)
                .addOnFailureListener(error ->
                        updateStatus("Could not send network candidate: "
                                + error.getMessage()));
    }

    private static class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sdp) {}

        @Override
        public void onSetSuccess() {}

        @Override
        public void onCreateFailure(String error) {}

        @Override
        public void onSetFailure(String error) {}
    }

    private void endCall() {
        shuttingDown = true;

        if (currentCall != null) {
            if (answerListener != null) {
                currentCall.child("answer")
                        .removeEventListener(answerListener);
            }

            if (candidateListener != null) {
                currentCall.child(isCaller
                                ? "calleeCandidates"
                                : "callerCandidates")
                        .removeEventListener(candidateListener);
            }
        }

        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }

        if (cameraCapturer != null) {
            try {
                cameraCapturer.stopCapture();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            cameraCapturer.dispose();
            cameraCapturer = null;
        }

        if (localVideoTrack != null) {
            localVideoTrack.dispose();
            localVideoTrack = null;
        }

        if (localAudioTrack != null) {
            localAudioTrack.dispose();
            localAudioTrack = null;
        }

        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }

        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }

        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        if (localRenderer != null) {
            localRenderer.release();
        }

        if (remoteRenderer != null) {
            remoteRenderer.release();
        }

        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }

        currentCall = null;
        callStarted = false;

        updateStatus("Call ended.");
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> {
            if (statusText != null) {
                statusText.setText(message);
            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        endCall();
        super.onDestroy();
    }
}

