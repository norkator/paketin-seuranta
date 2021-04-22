package com.nitramite.adapters;

import android.graphics.Canvas;
import android.util.Log;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NonNls;

// Origin https://www.androidhive.info/2017/09/android-recyclerview-swipe-delete-undo-using-itemtouchhelper/
public class RecyclerItemTouchHelper extends ItemTouchHelper.SimpleCallback {

    @NonNls
    private static final String TAG = RecyclerItemTouchHelper.class.getSimpleName();


    // Specifies view settings like swipe direction layout handling
    public enum ActivityTarget {
        MAIN_MENU,
        ARCHIVE,
    }

    private RecyclerItemTouchHelperListener listener;
    private ActivityTarget activityTarget;

    public RecyclerItemTouchHelper(int dragDirs, int swipeDirs, RecyclerItemTouchHelperListener listener, ActivityTarget activityTarget) {
        super(dragDirs, swipeDirs);
        this.listener = listener;
        this.activityTarget = activityTarget;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null) {
            final View foregroundView = ((ParcelsAdapter.ViewHolder) viewHolder).viewForeground;
            getDefaultUIUtil().onSelected(foregroundView);
        }
    }

    @Override
    public void onChildDrawOver(Canvas canvas, RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
        final View foregroundView = ((ParcelsAdapter.ViewHolder) viewHolder).viewForeground;

        getDefaultUIUtil().onDrawOver(canvas, recyclerView, foregroundView, dX, dY,
                actionState, isCurrentlyActive);
        switch (activityTarget) {
            case MAIN_MENU:
                if (dX > 0) {
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewArchive.setVisibility(View.VISIBLE);
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewDelete.setVisibility(View.GONE);
                    ViewCompat.setElevation(foregroundView, 30);
                } else if (dX < 0) {
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewDelete.setVisibility(View.VISIBLE);
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewArchive.setVisibility(View.GONE);
                    ViewCompat.setElevation(foregroundView, 30);
                } else {
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewArchive.setVisibility(View.GONE);
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewDelete.setVisibility(View.GONE);
                    ViewCompat.setElevation(foregroundView, 5);
                }
                break;
            case ARCHIVE:
                if (dX > 0) {
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewReturn.setVisibility(View.VISIBLE);
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewDelete.setVisibility(View.GONE);
                    ViewCompat.setElevation(foregroundView, 30);
                } else if (dX < 0) {
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewDelete.setVisibility(View.VISIBLE);
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewReturn.setVisibility(View.GONE);
                    ViewCompat.setElevation(foregroundView, 30);
                } else {
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewReturn.setVisibility(View.GONE);
                    ((ParcelsAdapter.ViewHolder) viewHolder).swipeViewDelete.setVisibility(View.GONE);
                    ViewCompat.setElevation(foregroundView, 5);
                }
                break;
        }

    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final View foregroundView = ((ParcelsAdapter.ViewHolder) viewHolder).viewForeground;
        getDefaultUIUtil().clearView(foregroundView);
    }

    @Override
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        final View foregroundView = ((ParcelsAdapter.ViewHolder) viewHolder).viewForeground;
        getDefaultUIUtil().onDraw(canvas, recyclerView, foregroundView, dX, dY,
                actionState, isCurrentlyActive);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAbsoluteAdapterPosition();
        listener.onSwiped(viewHolder, direction, position);
        //noinspection ConstantConditions
        ((ParcelsAdapter.ViewHolder) viewHolder).getBindingAdapter().notifyItemChanged(position);
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    public interface RecyclerItemTouchHelperListener {
        void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position);
    }
}