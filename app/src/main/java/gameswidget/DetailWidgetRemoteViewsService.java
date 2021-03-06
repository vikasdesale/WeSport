package gameswidget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.my.game.wesport.R;
import com.my.game.wesport.data.GameContract.GameEntry;

import java.util.concurrent.ExecutionException;

import static com.my.game.wesport.R.id.widget_icon;
import static com.my.game.wesport.data.GameContract.GameEntry.COLUMN_GAME_NAME;


/**
 * RemoteViewsService controlling the data being shown in the scrollable weather detail widget
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {
    // these indices must match the projection
    private static final int INDEX_GAME_ID = 0;
    private static final String[] GAME_COLUMNS = {
            GameEntry.TABLE_NAME + "." + GameEntry._ID,
            GameEntry.COLUMN_GAME_DESC,
            GameEntry.COLUMN_START_DATE,
            GameEntry.COLUMN_GAME_NAME,
            GameEntry.COLUMN_START_TIME,
            GameEntry.COLUMN_END_TIME,
            GameEntry.COLUMN_GAME_SKILL,
            GameEntry.COLUMN_GAME_ADDRESS,
            GameEntry.COLUMN_USER_NAME
    };
    private final Uri gameDataForUri = GameEntry.CONTENT_URI;

    @SuppressWarnings("unused")
    private String[] selectionArgs = null;
    private String username = "";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                // Get data from the ContentProvider
                username = GameEntry.getUserName(DetailWidgetRemoteViewsService.this);
                selectionArgs = new String[]{username};
                data = getContentResolver().query(gameDataForUri,
                        GAME_COLUMNS,
                        null,
                        null,
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @SuppressWarnings("UnusedAssignment")
            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                // Find the columns of game attributes that we're interested in
                int nameColumnIndex = data.getColumnIndex(GameEntry.COLUMN_GAME_DESC);
                int gamenameColumnIndex = data.getColumnIndex(COLUMN_GAME_NAME);
                int startDateIndex = data.getColumnIndex(GameEntry.COLUMN_START_DATE);
                int startTimeIndex = data.getColumnIndex(GameEntry.COLUMN_START_TIME);
                int endTimeIndex = data.getColumnIndex(GameEntry.COLUMN_END_TIME);
                int notesIndex = data.getColumnIndex(GameEntry.COLUMN_GAME_NOTES);
                int locColumnIndex = data.getColumnIndex(GameEntry.COLUMN_GAME_ADDRESS);

                Bitmap widgetImage = null;
                Bitmap glideImage = null;

                // Read the game attributes from the Cursor for the current game
                String selectedGame = data.getString(gamenameColumnIndex);
                String gameName = data.getString(nameColumnIndex);
                String stDate = data.getString(startDateIndex);
                String stTime = data.getString(startTimeIndex);
                String etTime = data.getString(endTimeIndex);
                String gameaddress = data.getString(locColumnIndex);
//              String notes = data.getString(notesIndex);

               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    setRemoteContentDescription(views, gameName);
                }

                Uri gameImageURI=null;
                switch (selectedGame) {
                    case "Basketball":
                        gameImageURI = Uri.parse("android.resource://" + DetailWidgetRemoteViewsService.this.getPackageName() + "/drawable/basketball");
                        break;
                    case "Cricket":
                        gameImageURI = Uri.parse("android.resource://" + DetailWidgetRemoteViewsService.this.getPackageName() + "/drawable/cricket");                //}

                        break;
                    case "Football":
                        gameImageURI = Uri.parse("android.resource://" + DetailWidgetRemoteViewsService.this.getPackageName() + "/drawable/football");                //}

                        break;
                    case "Tennis":
                        gameImageURI = Uri.parse("android.resource://" + DetailWidgetRemoteViewsService.this.getPackageName() + "/drawable/tennis");                //}

                        break;
                    case "Frisbee":
                        gameImageURI = Uri.parse("android.resource://" + DetailWidgetRemoteViewsService.this.getPackageName() + "/drawable/frisbee");                //}

                        break;
                    case "Pingpong":
                        gameImageURI = Uri.parse("android.resource://" + DetailWidgetRemoteViewsService.this.getPackageName() + "/drawable/pingpong");                //}

                        break;
                    case "Soccer":
                        gameImageURI = Uri.parse("android.resource://" + DetailWidgetRemoteViewsService.this.getPackageName() + "/drawable/soccer");                //}

                        break;
                    case "Volleyball":
                        gameImageURI = Uri.parse("android.resource://" + DetailWidgetRemoteViewsService.this.getPackageName() + "/drawable/volleyball");                //}

                        break;
                }
                    try {
                        glideImage = Glide.with(DetailWidgetRemoteViewsService.this)
                                .load(gameImageURI)
                                .asBitmap()
                                .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                if (glideImage != null) {
                    views.setImageViewBitmap(R.id.widget_icon, glideImage);
                }

                views.setTextViewText(R.id.widget_date, stDate);
                views.setTextViewText(R.id.widget_description, gameName);
                views.setTextViewText(R.id.widget_start_time, stTime);
                views.setTextViewText(R.id.widget_end_time, etTime);
                final Intent fillInIntent = new Intent();
                fillInIntent.setData(gameDataForUri);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            private void setRemoteContentDescription(RemoteViews views, String description) {
                views.setContentDescription(widget_icon, description);
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(INDEX_GAME_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}