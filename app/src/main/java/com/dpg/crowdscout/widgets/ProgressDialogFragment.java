package com.dpg.crowdscout.widgets;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.dpg.crowdscout.R;

public class ProgressDialogFragment extends DialogFragment {

    private static final String PROGRESS_DIALOG_ID = "progress";
    private static final String LOG_TAG = ProgressDialogFragment.class.getSimpleName();

    static ProgressDialogFragment s_progressDialogFragment;

    public static void show(FragmentManager fragmentManager) {
        show(fragmentManager, true);
    }

    public static void show(FragmentManager fragmentManager, boolean isCancellable) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to dismiss any currently showing
        // dialog, which can be accomplished using DialogFragment.dismiss
        Fragment prev = fragmentManager.findFragmentByTag(PROGRESS_DIALOG_ID);
        if (prev != null) {
            DialogFragment previousDialogFragment = (DialogFragment) prev;
            // Dismiss allowing state loss since we no longer care about the state of the previous dialog
            previousDialogFragment.dismissAllowingStateLoss();
        }

        try {
            // Create and show the dialog.
            s_progressDialogFragment = ProgressDialogFragment.newInstance();
            s_progressDialogFragment.setCancelable(isCancellable);
            s_progressDialogFragment.show(fragmentManager, PROGRESS_DIALOG_ID);
        } catch (IllegalStateException ise) {
            // An IllegalStateException will occur if this method was called after the host activity's onSaveInstanceState, therefore
            // this method should only be called when (activity.getLifeCycleState() == BaseActivity.LifeCycleState.RESUMED)
            Log.e(LOG_TAG, "IllegalStateException encountered by PickCollectionDialogFragment.show(): " + ise.getMessage(), ise);
        }
    }

    public static void hide() {
        if (s_progressDialogFragment != null) {
            if (s_progressDialogFragment.getDialog() != null) {
                s_progressDialogFragment.getDialog().dismiss();
            }
            s_progressDialogFragment.dismissAllowingStateLoss();
            s_progressDialogFragment = null;
        }
    }

    public static boolean isProgressDialogVisible() {
        return s_progressDialogFragment != null && s_progressDialogFragment.getDialog() != null && s_progressDialogFragment.getDialog().isShowing();
    }

    static ProgressDialogFragment newInstance() {
        ProgressDialogFragment progressDialogFragment = new ProgressDialogFragment();
        return progressDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getResources().getString(R.string.loading));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }
}