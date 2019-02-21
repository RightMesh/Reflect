package io.left.reflect;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import io.left.reflect.helper.MeshHelper;
import io.left.rightmesh.id.MeshId;

/**
 * A custom adapter to style the MeshIds a little nicer in the list.
 */
class MeshIdAdapter extends ArrayAdapter<MeshId> {

    /**
     * Id of the peer to treat as this device (i.e. for styling and naming).
     */
    private MeshId deviceId;

    /**
     * Inflates the parent {@link ArrayAdapter} and stores the context for use loading colours.
     *
     * @param context app context, need by parent class
     */
    MeshIdAdapter(@NonNull Context context) {
        super(context, android.R.layout.simple_spinner_dropdown_item);
    }

    void setDeviceId(MeshId deviceId) {
        this.deviceId = deviceId;
    }

    //
    // VIEW GENERATION METHODS
    //

    /**
     * Returns default view with colour/text modified by
     * {@link MeshIdAdapter#modifyView(TextView, int)}.
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        return modifyView(view, position);
    }

    /**
     * Returns default view with colour/text modified by
     * {@link MeshIdAdapter#modifyView(TextView, int)}.
     * {@inheritDoc}
     */
    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        return modifyView(view, position);
    }

    /**
     * Update the supplied view with a shortened MeshId string or the special styling for the
     * current device.
     *
     * @param view     view to be modified
     * @param position position of the item to be used
     * @return the modified view
     */
    private View modifyView(TextView view, int position) {
        MeshId item = this.getItem(position);
        if (item != null) {
            String text; // Text for the item in the list.
            int colour;  // Colour for the text in the list.

            if (item.equals(deviceId)) {
                // Change text colour if is the current device's Id.
                text = "This Device";
                colour = R.color.blue;
            } else {
                // Otherwise, simply make the MeshId more readable and use the theme default colour.
                text = MeshHelper.getInstance().shortenMeshId(item);
                colour = android.R.color.primary_text_light;
            }
            view.setText(text);
            view.setTextColor(ContextCompat.getColor(getContext(), colour));
        }
        return view;
    }


    //
    // HELPER METHODS
    //

    /**
     * Mimic {@link java.util.ArrayList#contains(Object)} behaviour to make this class easier to
     * use as a list.
     *
     * @param item to check the existence of
     * @return true if the provided item is in the array, false otherwise
     */
    boolean contains(MeshId item) {
        return getPosition(item) >= 0; // The position is -1 if it doesn't exist.
    }
}