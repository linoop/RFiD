package com.technowave.techno_rfid;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

/**
 * Created by Mikko on 22.9.2016.
 */
public class Beeper {

    public static final int BEEP_40MS = 0;
    public static final int BEEP_100MS = 1;
    public static final int BEEP_300MS = 2;
    public static final int FAIL = 3;
    private static final int LAST = 4;

    static SoundPool mSoundPool = null;
    static boolean mEnabled = true;

    static int[] mSoundIDs = new int[LAST];

    static public void setEnabled(boolean val) {
        mEnabled = val;
    }

    static public boolean getEnabled() {
        return mEnabled;
    }

    static public void beep(int type)
    {
        if (!mEnabled)
            return;

        if (mSoundPool == null)
            return;

        int id = mSoundIDs[type];
        try {
            mSoundPool.stop(id);
        } catch (Exception e) { }
        try {
            mSoundPool.play(id, 1, 1, 1, 0, 1);
        } catch (Exception e) { }
    }

    static public void init(Context ctx)
    {
        if (mSoundPool == null)
        {
            mSoundPool = new SoundPool(LAST, AudioManager.STREAM_MUSIC, 0);
            mSoundIDs[BEEP_40MS] = mSoundPool.load( ctx, R.raw.blep_40ms, 1);
            mSoundIDs[BEEP_100MS] = mSoundPool.load(ctx, R.raw.blep_100ms, 1);
            mSoundIDs[BEEP_300MS] = mSoundPool.load(ctx, R.raw.blep_300ms, 1);
            mSoundIDs[FAIL] = mSoundPool.load(ctx, R.raw.blipblipblip, 1);
        }
    }
}
