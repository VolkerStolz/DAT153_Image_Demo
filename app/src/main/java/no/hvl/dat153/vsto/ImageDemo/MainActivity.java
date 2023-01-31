package no.hvl.dat153.vsto.ImageDemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
            /* We're not allowed to access the network from the UI-thread: */
            new Thread() {
                @Override
                public void run() {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                        /* Let's be polite: */
                        connection.setRequestProperty("User-agent", "no.hvl.dat153.vsto/0.1");
                        connection.connect();
                        Bitmap bm = BitmapFactory.decodeStream(connection.getInputStream());
                        if (bm == null)
                            throw new RuntimeException();
                        /* But we need to return to the UI thread to update the image: */
                        boolean result = iv.post(new Runnable() {
                            @Override
                            public void run() {
                                iv.setImageBitmap(bm);
                            }
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

        images.add(new ResourceImage());
        images.add(new URLImage() {
            @Override
            String getName() { return "Erna Solberg"; }
            {
                url = "https://upload.wikimedia.org/wikipedia/commons/2/25/Erna_Solberg%2C_Wesenberg%2C_2011_%281%29.jpg";
            }
        });

        /* Should be a RecyclerView. */
        final GridView grid = findViewById(R.id.grid);

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