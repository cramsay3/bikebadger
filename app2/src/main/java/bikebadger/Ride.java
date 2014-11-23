package bikebadger;

import android.util.Log;

import java.text.DecimalFormat;
import java.util.Date;

import util.Constants;

public class Ride {
    private long mId;
    private Date mStartDate;
    private long mStartimeSeconds;
    private long mStopwatchSeconds;
    private long mPauseSeconds;
     private double mTargetAvgSpeed;
    private int mSpeedCount;
    private double mSpeedSum;
    private double mCurrentSpeed;
    public final static double METERS_TO_MILES = 2.2369;

    public Ride() {
        Log.d("Bike Badger","Ride::Ride()");
        mId = -1;
        mStartDate =  new Date();
        mPauseSeconds = mStopwatchSeconds =  mStartimeSeconds = System.currentTimeMillis() / 1000;
        Log.d("Bike Badger", "1) mStartDate.getTime()=" + mStartDate.getTime());

        mTargetAvgSpeed = -1;
        mSpeedCount = 0;
        mSpeedSum = 0;
        mCurrentSpeed = 0;
     }

   //public void PauseStopwatch() {
     //mStopwatch.setTime();
    //}

    public double GetTargetAvgSpeed() { return mTargetAvgSpeed; }
    public void SetTargetAvgSpeed( double targetAvgSpeed) { mTargetAvgSpeed = targetAvgSpeed;}
    public void SetTargetAvgSpeed( String sTargetAvgSpeed, double defaultTargetAvgSpeed)
    {
        mTargetAvgSpeed = defaultTargetAvgSpeed;
        try {
            DecimalFormat dF = new DecimalFormat("#0.0");
            Number num = dF.parse(sTargetAvgSpeed);
            mTargetAvgSpeed = num.doubleValue();
            Log.d("RideManager", String.format("Target speed is %.1f miles per hour.", mTargetAvgSpeed));
        } catch (Exception e) {
            Log.d("RunTracker","Error parsing DecimalFormat for mTargetAvgSpeed");
        }

    }
    public long GetId() {
        return mId;
    }

    public double UpdateAverageSpeed(double speed /* in feet */)
    {
        mCurrentSpeed = speed;
        mSpeedCount += 1;
        mSpeedSum += mCurrentSpeed;

        return mSpeedSum / mSpeedCount; // return average speed
    }

    public double GetAverageSpeed()
    {
        double avgSpeed = 0;
        if(mSpeedSum > 0)
            avgSpeed = (mSpeedSum / mSpeedCount);

        return avgSpeed;
    }

    public void ResetAverageSpeed()
    {
        mSpeedSum = 0;
        mSpeedCount = 0;
    }

    public double BehindTargetSpeed()
    {
        double avgSpeed = GetAverageSpeed();
        double delta = mTargetAvgSpeed - avgSpeed;
        return delta;
    }
    public void SetId(long id) {
        mId = id;
    }

    public Date getStartDate() {
        return mStartDate;
    }

    public void setStartDate(Date startDate) {
        mStartDate = startDate;
    }

    public void StartStopwatch() {
        mStopwatchSeconds = System.currentTimeMillis() / 1000;
    }

    public void PauseStopwatch() {
        mPauseSeconds = System.currentTimeMillis() / 1000;
    }

    public int StopwatchSeconds() {
        long startTimeSecs;
        long endSecs;
        int inSeconds1; // TODO if clock not set to auto update you can be off
        int inSeconds2;



        endSecs = System.currentTimeMillis() / 1000;

       // inSeconds1 = (int)(endSecs - mStartimeSeconds);
        //int temp = (int)(mStopwatchSeconds - mPauseSeconds);
        //inSeconds2 = inSeconds1 + temp;

        inSeconds1 = (int)(endSecs - mStopwatchSeconds);
        int temp = (int)(mPauseSeconds - mStartimeSeconds);
        inSeconds2 = inSeconds1 + temp;

       Log.i(Constants.APP.TAG, "endSecs - mStopwatchSeconds=" + inSeconds1 + " mPauseSeconds - mStartimeSeconds=" + temp +  " final result=" + inSeconds2 + " mPauseSeconds=" + mPauseSeconds);

        if(inSeconds2 < 0)
            return 0;
        else
            return inSeconds2;

    }

    public int GetDurationSeconds(long endMillis) {

        /*
        startTimeSecs = mStartDate.time

         */
        long startTimeSecs;
        long endSecs;
        int inSeconds; // TODO if clock not set to auto update you can be off

        startTimeSecs = mStartDate.getTime() / 1000;
        endSecs = endMillis / 1000;
        inSeconds = (int)(endSecs - startTimeSecs);

        if(inSeconds < 0)
            return 0;
        else
            return inSeconds;
    }

}
