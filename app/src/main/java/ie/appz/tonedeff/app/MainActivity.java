package ie.appz.tonedeff.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnItemClick;


public class MainActivity extends Activity {
    private final float duration = 0.15f; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = (int) (duration * sampleRate);
    private final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * numSamples, AudioTrack.MODE_STREAM);
    private final Double pianoFrequencyPeak = Double.valueOf(3600 - 50);
    @InjectView(R.id.gridView)
    GridView gridView;
    private ColorAdapter colorAdapter = new ColorAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        gridView.setAdapter(colorAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_play) {
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < colorAdapter.getCount(); i++) {
                        final int j = i;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                gridView.smoothScrollToPosition(j);
                                gridView.requestFocusFromTouch();
                                gridView.setSelection(j);
                                gridView.performItemClick(gridView.getAdapter().getView(j, null, null), j, gridView.getAdapter().getItemId(j));

                            }
                        });
                        synchronized (this) {
                            try {
                                this.wait((long) (duration * 1000));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            })).start();
        }
        return super.onOptionsItemSelected(item);
    }


    @OnItemClick(R.id.gridView)
    void gridItemClicked(View view, int position) {
        ColorDrawable colorDrawable = (ColorDrawable) ((ImageView) view).getDrawable();
        int val = 0xffffffff + colorDrawable.getColor();
        val = val - 0xaa000000;
        int colorAvg = (Color.red(val) + Color.blue(val) + Color.red(val)) / 3;

        double colorOffset = (double) colorAvg / (double) 0xff;
        double pianoFreq = colorOffset * pianoFrequencyPeak + 50;
        Log.d("AP1", "Color: " + colorDrawable.getColor() + " - " + colorDrawable.getOpacity() + " - " + colorDrawable.getAlpha() + " - " + val + " - " + colorOffset + " - " + pianoFreq);
        byte[] tone = genTone(pianoFreq);

        audioTrack.write(tone, 0, tone.length);

        audioTrack.play();
    }

    private byte[] genTone(double frequency) {
        byte generatedSnd[] = new byte[2 * numSamples];
        final double sample[] = new double[numSamples];
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / frequency));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
        return generatedSnd;
    }


    private class ColorAdapter extends BaseAdapter {
        final int TAIL = 0;
        final int ITEM = 1;
        ArrayList<Integer> colorList = new ArrayList<Integer>();

        public ColorAdapter() {
            Time seedTime = new Time();
            seedTime.setToNow();
            Random randomColor = new Random(seedTime.toMillis(true));
            for (int i = 0; i < 24; i++) {
                colorList.add(0xaa000000 + randomColor.nextInt(0xFFFFFF));
            }


        }

        @Override
        public int getCount() {
            return colorList.size();
        }

        @Override
        public Object getItem(int position) {
            return colorList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getItemViewType(int i) {
            if (i == colorList.size() - 1)
                return TAIL;
            else
                return ITEM;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
           /* switch (getItemViewType(position)) {
                case ITEM:{
*/
            if (convertView == null) {
                convertView = View.inflate(getBaseContext(), R.layout.colored_view, null);
                ((ImageView) convertView).setAdjustViewBounds(true);
            }
            ColorDrawable colorDrawable = new ColorDrawable();
            colorDrawable.setColor(colorList.get(position));
            ((ImageView) convertView).setImageDrawable(colorDrawable);
            return convertView;

         /*   }
                case TAIL:{

                }
            }*/
        }
    }
}
