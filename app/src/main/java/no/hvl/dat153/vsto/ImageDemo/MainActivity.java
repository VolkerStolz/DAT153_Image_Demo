package no.hvl.dat153.vsto.ImageDemo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    abstract class ImageItem {
        abstract String getName();
        abstract void setImage(ImageView iv);
    }

    class ResourceImage extends ImageItem {

        @Override
        String getName() {
            return "Jens Stoltenberg";
        }

        @Override
        void setImage(ImageView iv) {
            iv.setImageDrawable(getResources().getDrawable(R.drawable.stoltenberg));
        }
    }

    abstract class URLImage extends ImageItem {

        String url;

        @Override
        void setImage(ImageView iv) {
            /* Show a place-holder while we're fetching the image: */
            iv.setImageDrawable(getResources().getDrawable(R.drawable.baseline_question_mark_24));
            /* We're not allowed to access the network from the UI-thread: */
            new Thread() {
                @Override
                public void run() {
                    try {
                        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
                        /* Let's be polite: */
                        connection.setRequestProperty("User-agent", "no.hvl.dat153.vsto/0.1");
                        connection.connect();
                        /* Uncomment this line to actually see the placeholder: */
                        // sleep(5000);
                        Bitmap bm = BitmapFactory.decodeStream(connection.getInputStream());
                        connection.disconnect();
                        if (bm == null)
                            throw new RuntimeException();
                        /* But we need to return to the UI thread to update the image: */
                        boolean result = iv.post(new Runnable() {
                            @Override
                            public void run() { iv.setImageBitmap(bm); }
                        });
                        if (!result) throw new RuntimeException();
                    } catch (Exception e) {
                        Log.e("vsto", "Something didn't work :-(", e);
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
    }

    Set<ImageItem> images = new HashSet<ImageItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i("vsto", "HTTP response cache installation failed:" + e);
        }

        images.add(new ResourceImage());
        images.add(new URLImage() {
            @Override
            String getName() { return "Erna Solberg"; }
            {
                url = "https://upload.wikimedia.org/wikipedia/commons/2/25/Erna_Solberg%2C_Wesenberg%2C_2011_%281%29.jpg";
            }
        });

        /* Should be a RecyclerView. */
        final ListView grid = findViewById(R.id.grid);

        ArrayAdapter<ImageItem> a = new ArrayAdapter<ImageItem>(this, R.layout.personitem) {
            public View getView(int position, View _convertView, ViewGroup parent){
                LinearLayout view = (LinearLayout) LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.personitem, null);
                ImageView iv = view.findViewById(R.id.image_data);
                TextView tv = view.findViewById(R.id.image_name);

                this.getItem(position).setImage(iv);
                tv.setText(this.getItem(position).getName());
                return view;
            }
        };
        a.addAll(images);

        grid.setAdapter(a);
    }
}