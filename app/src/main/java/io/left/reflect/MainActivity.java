package io.left.reflect;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.left.reflect.helper.MeshHelper;
import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.id.MeshId;
import io.left.rightmesh.mesh.MeshManager.DataReceivedEvent;
import io.left.rightmesh.mesh.MeshManager.RightMeshEvent;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.RightMeshException;
import io.left.rightmesh.util.RightMeshException.RightMeshServiceDisconnectedException;

import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;

/**
 * Simple app for testing RightMesh network reach.
 *
 * <p>
 * Device A sends a ping to Device B over a mesh network containing a unique message (a
 * simple timestamp). Device B receives it and echoes it back. Device A displays when the
 * echoed message is received. This makes it easy to leave a device in one spot, roam around
 * with another device, and test if the network still works.
 * </p>
 */
public class MainActivity extends AppCompatActivity implements MeshStateListener,
        RightMeshRecipientView.RecipientChangedListener{
    private static final String TAG = MainActivity.class.getCanonicalName();

    

    AndroidMeshManager meshManager;

    // Id of this device, stored for UI use.
    MeshId deviceId;

    // Id of the peer to send pings to.
    MeshId recipientId;

    // Responsible for allowing the user to select the ping recipient.
    RightMeshRecipientView viewRightMeshRecipient;

    // Adapter for tracking views and the spinner it feeds, both mostly powered by `viewRightMeshRecipient`.
    MeshIdAdapter peersListAdapter;
    Spinner peersListView;

    // List and adapter tracking sent pings and whether or not they have been echoed.
    ArrayList<String> pingsList;
    ArrayAdapter<String> pingsListAdapter;

    static final char ALREADY_ECHOED = '0';
    static final char ECHO = '1';

    public MainActivity() {
        pingsListAdapter = null;
        pingsList = new ArrayList<>();
        peersListView = null;
        peersListAdapter = null;
        recipientId = null;
        deviceId = null;
        meshManager = null;
    }


    //
    // ANDROID LIFECYCLE & EVENT HANDLING
    //

    /**
     * Initializes list adapters, sets UI event handlers, and starts the RightMesh library
     * connection on application start.
     *
     * @param savedInstanceState passed by Android
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Send a ping when the floating send button is tapped.
        View buttonSend = findViewById(R.id.button_send);
        buttonSend.setOnClickListener(this::sendPing);

        // Display the RightMesh settings activity when the send button is tapped and held.
        buttonSend.setLongClickable(true);
        buttonSend.setOnLongClickListener(v -> {
            try {
                meshManager.showSettingsActivity();
            } catch (RightMeshServiceDisconnectedException sde) {
                Log.e(TAG, "Service disconnected while displaying settings activity, with message: "
                        + sde.getMessage());
            } catch (RightMeshException rme) {
                Log.e(TAG, "Some other error occurred while displaying settings activity, with "
                        + "message: " + rme.getMessage());
            }
            return false;
        });

        // Set up the recipient selection spinner.
        peersListAdapter = new MeshIdAdapter(this);
        viewRightMeshRecipient = findViewById(R.id.view_rightmesh_recipient);
        viewRightMeshRecipient.setSpinnerAdapter(peersListAdapter);
        viewRightMeshRecipient.setOnRecipientChangedListener(this);
        peersListView = findViewById(R.id.spinner_recipient);

        // Set up the rvLogs list.
        pingsListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pingsList);
        ListView rvLogs = findViewById(R.id.recyclerview_logs);
        rvLogs.setAdapter(pingsListAdapter);

        // Initialize the mesh.
        meshManager = AndroidMeshManager.getInstance(MainActivity.this, MainActivity.this);
    }

    /**
     * Send a ping to the recipient.
     *
     * @param view passed by Android
     */
    private void sendPing(View view) {
        DateFormat df = new SimpleDateFormat("MMM dd kk:mm:ss:SSSS");
        // DateFormat df = DateFormat.getDateInstance();

        // Null check, as recipientId has no default value.
        if (recipientId != null) {
            // Ping content is just the current time, so they are unique and give us some rough
            // concept of delay.
            String timestamp = df.format(new Date());
            String payload = "1" + timestamp;

            try {
                // Attempt to ping the currently selected recipient.
                meshManager.sendDataReliable(recipientId, Constants.MESH_PORT, payload.getBytes());

                // Log the ping if sent successfully.
                pingsList.add(0, timestamp);
                runOnUiThread(() -> pingsListAdapter.notifyDataSetChanged());
            } catch (RightMeshServiceDisconnectedException sde) {
                Log.e(TAG, "Service disconnected before ping could be sent, with message: "
                        + sde.getMessage());
            } catch (RightMeshException rme) {
                Log.e(TAG, "Error occurred sending ping, with message: " + rme.getMessage());
            }
        }
    }

    /**
     * Fired by the {@link RightMeshRecipientView} when the selected recipient Id has changed.
     *
     * <p>Stores the new recipient and updates the display.</p>
     *
     * @param recipient new recipient
     */
    @Override
    public void onRecipientChanged(MeshId recipient) {
        recipientId = recipient;
        updateRecipientColour();
    }


    //
    // MESH EVENT HANDLING
    //

    /**
     * Configures event handlers, binds to a port, and updates the label when the RightMesh library
     * is ready.
     *
     * @param meshId Id of this device
     * @param state  new state of the RightMesh library
     */
    @Override
    public void meshStateChanged(MeshId meshId, int state) {
        deviceId = meshId;
        if (state == MeshStateListener.SUCCESS) {
            try {
                // Bind to a port.
                meshManager.bind(Constants.MESH_PORT);

                // Initialize the peer adapter with this device's MeshId.
                peersListAdapter.add(deviceId);
                peersListAdapter.setDeviceId(deviceId);
                runOnUiThread(() -> peersListAdapter.notifyDataSetChanged());

                // Bind RightMesh event handlers.
                meshManager.on(DATA_RECEIVED, this::receiveData);
                meshManager.on(PEER_CHANGED, this::updateColoursOnPeerChanged);
                meshManager.on(PEER_CHANGED, rme -> runOnUiThread(() -> viewRightMeshRecipient.updatePeersList(rme)));

                TextView libraryStatus = findViewById(R.id.text_view_device_status);
                libraryStatus.setText("Library has started with MeshId: " + meshManager.getUuid().toString());

            } catch (RightMeshServiceDisconnectedException sde) {
                Log.e(TAG, "Service disconnected while binding, with message: " + sde.getMessage());
            } catch (RightMeshException rme) {
                Log.e(TAG, "Error occurred binding port, with message: " + rme.getMessage());
            }
        }
    }

    /**
     * Returns a ping, or logs a returned ping.
     *
     * @param rme Event passed from RightMesh.
     */
    private void receiveData(RightMeshEvent rme) {
        // Parse data from event.
        DataReceivedEvent dre = (DataReceivedEvent) rme;
        String dataString = new String(dre.data);
        char echoBit = dataString.charAt(0); // `1` for initial requests, `0` for echoed requests.
        String timestamp = dataString.substring(1);

        if (echoBit == ECHO) {
            // Echo messages starting with '1'.
            String responsePayload = "0" + dataString.substring(1);
            try {
                meshManager.sendDataReliable(dre.peerUuid, Constants.MESH_PORT, responsePayload.getBytes());
                pingsList.add(0, "Echoed ping. (" + MeshHelper.getInstance().shortenMeshId(rme.peerUuid) + ")");
            } catch (RightMeshServiceDisconnectedException sde) {
                Log.e(TAG, "Service disconnected before ping could be returned, with message: "
                        + sde.getMessage());
            } catch (RightMeshException rmx) {
                Log.e(TAG, "Error occurred sending ping, with message: " + rmx.getMessage());
            }
        } else if (echoBit == ALREADY_ECHOED) {
            // Messages starting with '0' have already been echoed - update log.
            if (pingsList.contains(timestamp)) {
                pingsList.set(pingsList.indexOf(timestamp),
                        timestamp + " - Received! (" + MeshHelper.getInstance().shortenMeshId(rme.peerUuid) + ")");
            }
        }

        // Null-check the adapter, as events may fire when the activity doesn't exist.
        if (pingsListAdapter != null) {
            runOnUiThread(() -> pingsListAdapter.notifyDataSetChanged());
        }
    }

    /**
     * Updates the colour of the selected peer every time its connection state changes.
     *
     * @param rme event passed from RightMesh
     */
    private void updateColoursOnPeerChanged(RightMeshEvent rme) {
        if (rme.peerUuid.equals(recipientId)) {
            updateRecipientColour();
        }
    }

    /**
     * Resume RightMesh connection on activity resume.
     */
    @Override
    protected void onResume() {
        super.onResume();
        try {
            meshManager.resume();
        } catch (RightMeshServiceDisconnectedException e) {
            Log.e(TAG, "Service disconnected before resuming AndroidMeshManager, with message: "
                    + e.getMessage());
        }
    }

    /**
     * Close RightMesh connection when activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            meshManager.stop();
        } catch (RightMeshServiceDisconnectedException e) {
            Log.e(TAG, "Service disconnected before stopping AndroidMeshManager, with message: "
                    + e.getMessage());
        }
    }

    //
    // HELPER FUNCTIONS
    //

    /**
     * Makes the recipient colour green if the recipient is connected, red if it isn't, or leaves it
     * blue if the recipient is the current device.
     */
    private void updateRecipientColour() {
        if (!recipientId.equals(deviceId)) {
            int colour;
            if (peersListAdapter.contains(recipientId)) {
                colour = R.color.green;
            } else {
                colour = R.color.red;
            }
            runOnUiThread(() -> ((TextView) peersListView.getSelectedView())
                    .setTextColor(ContextCompat.getColor(this, colour)));
        }
    }
}