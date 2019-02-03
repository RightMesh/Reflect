package io.left.reflect;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import io.left.rightmesh.id.MeshId;
import io.left.rightmesh.mesh.MeshManager.PeerChangedEvent;
import io.left.rightmesh.mesh.MeshManager.RightMeshEvent;

import static io.left.rightmesh.mesh.MeshManager.ADDED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;

/**
 * Custom view that keeps track of connected peers when registered to listen to PEER_CHANGED events,
 * and allows the user to select one of these peers as a message recipient.
 */
public class RightMeshRecipientView extends ConstraintLayout
        implements AdapterView.OnItemSelectedListener {

    // Keeps track of the most recently tracked recipient, in case it disconnects and is removed
    // from the list.
    private MeshId recipientId;

    // UI Elements
    private Spinner spinner;
    private TextView deviceStatusLabel;
    private TextView networkStatusLabel;

    // Keeps track of peers and populates the spinner.
    private MeshIdAdapter spinnerAdapter;

    private RecipientChangedListener onRecipientChangedListener = null;

    /**
     * Used when instantiating Views programmatically.
     *
     * @param context View context
     */
    public RightMeshRecipientView(Context context) {
        super(context);
        init(context);
    }

    /**
     * {@link RightMeshRecipientView} constructor<br>.
     *
     * Trigger in xml declaration.
     *
     * @param context View context
     * @param attrs attribute
     */
    public RightMeshRecipientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * {@link RightMeshRecipientView} constructor<br>.
     *
     * Trigger in xml declaration with android:style attribute
     *
     * @param context View context
     * @param attrs Attribute
     * @param defStyleAttr applied style
     */
    public RightMeshRecipientView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * Init view.
     * @param context View context
     */
    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        inflater.inflate(R.layout.customview_component_rightmesh, this, true);

        this.recipientId = null;

        spinner = findViewById(R.id.spinner_recipient);
        spinner.setOnItemSelectedListener(this);

        deviceStatusLabel = findViewById(R.id.text_view_device_status);
        networkStatusLabel = findViewById(R.id.textview_network_status);
    }

    /**
     * Get recipient id.
     * @return MeshId
     */
    public MeshId getRecipientId() {
        return recipientId;
    }

    /**
     * Set adapter of spinner.
     * @param spinnerAdapter Spinner Adapter
     */
    public void setSpinnerAdapter(MeshIdAdapter spinnerAdapter) {
        this.spinnerAdapter = spinnerAdapter;
        spinner.setAdapter(spinnerAdapter);
    }

    /**
     * Sets the contents of the status label (runs on the activity's UI thread).
     *
     * @param status new contents of the status label
     */
    public void setStatus(String status) {
        deviceStatusLabel.setText(status);
    }

    /**
     * Set a listener for updates to the value of the selected recipient.
     *
     * @param listener listener to be notified
     */
    public void setOnRecipientChangedListener(RecipientChangedListener listener) {
        onRecipientChangedListener = listener;
    }

    /**
     * When the selected item in the recipient selection spinner changes, update the local variable
     * and notify the listener.
     *
     * @param parent   the spinner
     * @param view     view of the selected peer
     * @param position position of the selected peer in the list, used to get the actual object
     * @param id       passed by Android
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        recipientId = spinnerAdapter.getItem(position);
        onRecipientChangedListener.onRecipientChanged(recipientId);
    }

    /**
     * If nothing is selected in the recipient selection spinner, and there are peers aside from
     * this device to select, arbitrarily select the first such peer in the list.
     *
     * @param parent the spinner
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        if (spinnerAdapter.getCount() > 1) {
            parent.setSelection(1);
        }
    }

    /**
     * Update the mesh peers available in the recipient selection spinner when mesh peers are
     * discovered or change state.
     *
     * @param rme event passed from RightMesh
     */
    public void updatePeersList(RightMeshEvent rme) {
        PeerChangedEvent pce = (PeerChangedEvent) rme;
        MeshId peer = rme.peerUuid;

        if (pce.state == ADDED && !spinnerAdapter.contains(peer)) {
            // Add the peer to the list if it is new.
            spinnerAdapter.add(peer);

            if (spinnerAdapter.getCount() == 2) {
                // If this is the first peer in the list, automatically select it.
                spinnerAdapter.notifyDataSetChanged();
                spinner.setSelection(1);
            }
        } else if (pce.state == REMOVED) {
            // Remove a peer when it disconnects.
            spinnerAdapter.remove(peer);

            // Toast if the recipient has been disconnected.
            if (peer.equals(recipientId)) {
                Toast.makeText(getContext(),
                        "Recipient has disconnected.", Toast.LENGTH_SHORT).show();
            }
        }

        // Update the connected devices label if there are other devices connected.
        if (spinnerAdapter.getCount() > 1) {
            // Get string resource with number of connected devices.
            int numConnectedDevices = spinnerAdapter.getCount() - 1;
            String newText = getResources().getQuantityString(
                    R.plurals.number_of_connected_devices,
                    numConnectedDevices, numConnectedDevices);

            networkStatusLabel.setText(newText);
        } else {
            networkStatusLabel.setText("");
        }
    }

    public interface RecipientChangedListener {
        /**
         * When the selected recipient Id has changed.
         *
         * Stores the new recipient and updates the display.
         *
         * @param recipient new recipient
         */
        void onRecipientChanged(MeshId recipient);
    }
}