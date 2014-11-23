package bikebadger;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import util.Constants;

/**
 * Created by cramsay on 10/11/2014.
 */
public class ActionCommand {
    private Date dtg;
    private int id;
    private String command;
    private String argument;
    private ArrayList<String> aliases;
    private Runnable runnable;
    private final Handler handler = new Handler();
    private RideManager rideManager;

    public static ArrayList<ActionCommand> actionCommands = null;
    public static void AddActionCommand(ActionCommand ac) {
        actionCommands.add(ac);
    }
    public static ActionCommand GetActionCommand(String name) {
        ActionCommand ac = null;

       // int id = -1;

        for(int idx = 0; actionCommands != null && idx < actionCommands.size(); idx++) {
            if(actionCommands.get(idx).Matches(name)) {
               return  actionCommands.get(idx);
            }
        }

        return ac;
    }


    public boolean Matches(String name)
    {
        String normalizedName = name.toUpperCase().trim();
        return ( command.equals(normalizedName) );
    }

    public ActionCommand(Context c, String command, List<String> aliases) {
        this.command = command;
        this.aliases = (ArrayList<String>) aliases;
        this.runnable = runnable;
        rideManager = RideManager.get(c);
    }

    public Runnable CreateRunnable(final String argument) {

        Runnable runnable = new Runnable() {
            public void run() {
                Log.d(Constants.APP.TAG, "Runnable argument=" + argument + ", "+ getArgument() );
            }
        };

        return runnable;
    }

    public void ExecuteAction(final String argument) {
        runnable = CreateRunnable(argument);
        handler.post(runnable);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }

    public ArrayList<String> getAliases() {
        return aliases;
    }

    public void setAliases(ArrayList<String> aliases) {
        this.aliases = aliases;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }
}
