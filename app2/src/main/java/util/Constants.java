package util;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Created by cramsay on 9/13/2014.
 */
public interface Constants {

    public interface APP {
        public static final String TAG = "Bike Badger";
        //public static final int FILE_CHOOSER_REQUEST_CODE = 6384; // onActivityResult request
        public static final int ACTION_WAYPOINT_REQUEST_CODE = 9090;
        public static final File EXTERNAL_APP_DIR = new File(Environment.getExternalStorageDirectory() + java.io.File.separator + "BikeBadger");
        public static final String GPX_11_SIMPLEDATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        public static final String GPX_01_MODIFIED_SIMPLEDATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        public final SimpleDateFormat SIMPLE_DTG_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        public static final String  DEFAULT_GPX_PATH = EXTERNAL_APP_DIR.getAbsolutePath() + "/empty.gpx";
        public static final String[] WAYPOINT_COMMANDS = { "PLAYLIST", "PROMPT", "MUTE", "SPEED", "START", "STRAVA REC", "STRAVA STOP", "VOLUME", "TASKER" };
        public static final long ONE_DAY = 24 * 60 * 60 * 1000;
        public static final long EXPIRY_DAYS = 7 ;
    }

    public interface DB {

        public interface ACTIVITY {
            public static final String TABLE = "activity";
            public static final String START_TIME = "start_time";
            public static final String DISTANCE = "distance";
            public static final String TIME = "time";
            public static final String NAME = "name";
            public static final String COMMENT = "comment";
            public static final String SPORT = "type";
            public static final String MAX_HR = "avg_hr";
            public static final String AVG_HR = "max_hr";
            public static final String AVG_CADENCE = "avg_cadence";
            public static final int SPORT_RUNNING = 0;
            public static final int SPORT_BIKING = 1;
            public static final int SPORT_OTHER = 2; // unknown
        };

        public interface LOCATION {
            public static final String TABLE = "location";
            public static final String ACTIVITY = "activity_id";
            public static final String LAP = "lap";
            public static final String TYPE = "type";
            public static final String TIME = "time";
            public static final String LATITUDE = "latitude";
            public static final String LONGITUDE = "longitude";
            public static final String ACCURANCY = "accurancy";
            public static final String ALTITUDE = "altitude";
            public static final String SPEED = "speed";
            public static final String BEARING = "bearing";
            public static final String HR = "hr";
            public static final String CADENCE = "cadence";

            public static final int TYPE_START = 1;
            public static final int TYPE_END = 2;
            public static final int TYPE_GPS = 3;
            public static final int TYPE_PAUSE = 4;
            public static final int TYPE_RESUME = 5;
            public static final int TYPE_DISCARD = 6;
        };

        public interface LAP {
            public static final String TABLE = "lap";
            public static final String ACTIVITY = "activity_id";
            public static final String LAP = "lap";
            public static final String INTENSITY = "type";
            public static final String TIME = "time";
            public static final String DISTANCE = "distance";
            public static final String PLANNED_TIME = "planned_time";
            public static final String PLANNED_DISTANCE = "planned_distance";
            public static final String PLANNED_PACE = "planned_pace";
            public static final String AVG_HR = "avg_hr";
            public static final String MAX_HR = "max_hr";
            public static final String AVG_CADENCE = "avg_cadence";
        };

        public interface INTENSITY {
            public static final int ACTIVE = 0;
            public static final int RESTING = 1;
            public static final int WARMUP = 2;
            public static final int COOLDOWN = 3;
            public static final int REPEAT = 4;
            public static final int RECOVERY = 5;
        };

        public interface ACCOUNT {
            public static final String TABLE = "account";
            public static final String NAME = "name";
            public static final String URL = "url";
            public static final String DESCRIPTION = "description";
            public static final String FORMAT = "format";
            public static final String FLAGS = "default_send";
            public static final String ENABLED = "enabled";
            public static final String AUTH_METHOD = "auth_method";
            public static final String AUTH_CONFIG = "auth_config";
            public static final String ICON = "icon";

            public static final int FLAG_UPLOAD = 0;
            public static final int FLAG_FEED = 1;
            public static final int FLAG_LIVE = 2;
            public static final int FLAG_SKIP_MAP = 3;
            public static final long DEFAULT_FLAGS =
                    (1 << FLAG_UPLOAD) +
                            (1 << FLAG_FEED) +
                            (1 << FLAG_LIVE);
        };

        public interface EXPORT {
            public static final String TABLE = "report";
            public static final String ACTIVITY = "activity_id";
            public static final String ACCOUNT = "account_id";
            public static final String STATUS = "status";
            public static final String EXTERNAL_ID = "ext_id";
            public static final String EXTRA = "extra";
        }

        public interface AUDIO_SCHEMES {
            public static final String TABLE = "audio_schemes";
            public static final String NAME = "name";
            public static final String SORT_ORDER = "sort_order";
        }

        public interface FEED {
            public static final String TABLE = "feed";
            public static final String ACCOUNT_ID = "account_id";
            public static final String EXTERNAL_ID = "ext_id"; // ID per account
            public static final String FEED_TYPE = "entry_type";
            public static final String FEED_SUBTYPE = "type";
            public static final String FEED_TYPE_STRING = "type_string";
            public static final String START_TIME = "start_time";
            public static final String DURATION = "duration";
            public static final String DISTANCE = "distance";
            public static final String USER_ID = "user_id";
            public static final String USER_FIRST_NAME = "user_first_name";
            public static final String USER_LAST_NAME = "user_last_name";
            public static final String USER_IMAGE_URL = "user_image_url";
            public static final String NOTES = "notes";
            public static final String COMMENTS = "comments";
            public static final String URL = "url";
            public static final String FLAGS = "flags";

            public static final int FEED_TYPE_ACTIVITY = 0; // FEED_SUBTYPE
            // contains
            // activity.type
            public static final int FEED_TYPE_EVENT = 1;

            public static final int FEED_TYPE_EVENT_DATE_HEADER = 0;
        }
    };

}
