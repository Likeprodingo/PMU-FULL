package com.example.firebaselab;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements PostsAdapter.OnPostListener{

    public static boolean sqlite = true;
    public static boolean admin = false;


    private RecyclerView postsList;
    private PostsAdapter postsAdapter;
    private Fragment fragment;
    FloatingActionButton fab;

    public static SQLiteDatabase db;
    public static DatabaseReference fDatabase;

    public static Point size;

    public List<Post> posts;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = getApplicationContext();
        final FragmentManager fragmentManager = getSupportFragmentManager();
        posts = new ArrayList<>();

        postsList = findViewById(R.id.rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        postsList.setLayoutManager(layoutManager);

        Intent intent = getIntent();
        if (intent.hasExtra("database")) {
            String database = intent.getStringExtra("database");
            if (database.equals("firebase"))
                sqlite = false;
            else
                sqlite = true;
        }



        if (sqlite) {

            db = new PostsSQLite.PostsReaderDbHelper(context).getWritableDatabase();
            try {
                loadPostsFromSQlite();
            } catch (MalformedURLException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            if (intent.hasExtra("update")) {
                try {
                    loadPostsFromSQlite();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (intent.hasExtra("position")) {
                int pos = intent.getIntExtra("position", -1);
                for(Post p : posts) {
                    Log.d("post before:" , p.title);
                }
                Log.d("size before:", posts.size() + "");
                posts.remove(pos);
                db.execSQL("DELETE FROM posts");
                Log.d("size after:", posts.size() + "");
                Collections.reverse(posts);
                for(Post p : posts) {
                    ContentValues values = new ContentValues();
                    values.put(PostsSQLite.PostsEntry.COLUMN_NAME_TITLE, p.title);
                    if (p.getDescription() != null)
                        values.put(PostsSQLite.PostsEntry.COLUMN_NAME_DESCRIPTION, p.description);
                    if (p.getImagePath() != null)
                        values.put(PostsSQLite.PostsEntry.COLUMN_NAME_IMAGE_PATH, p.getImagePath());
                    long newRowId = db.insert(PostsSQLite.PostsEntry.TABLE_NAME, null, values);
                }
                posts.clear();
                try {
                    loadPostsFromSQlite();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onPostClick(int position) {
        Post post = posts.get(position);
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        bundle.putString("title", post.title);
        bundle.putString("description", post.description);
        if (post.getImagePath() != null)
            bundle.putString("imagePath", post.imagePath);
        if (post.getSongPath() != null)
            bundle.putString("songPath", post.songPath);
        if (!post.getImageUrl().isEmpty()) {
            bundle.putString("imageUrl", post.imageUrl);
        }
        ShowPost postFragment = new ShowPost();
        postFragment.setArguments(bundle);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.host_activity, postFragment);
        fragmentTransaction.commit();
        fab.setVisibility(View.INVISIBLE);
    }

    void reloadPostsFromSQLite() {
        posts.clear();
        String[] projection = {
                PostsSQLite.PostsEntry.COLUMN_NAME_TITLE,
                PostsSQLite.PostsEntry.COLUMN_NAME_DESCRIPTION,
                PostsSQLite.PostsEntry.COLUMN_NAME_IMAGE_PATH
        };

        Cursor cursor = db.query(PostsSQLite.PostsEntry.TABLE_NAME, projection, null, null, null, null, null);
        while (cursor.moveToNext()) {
            Post p = new Post();
            p.title = cursor.getString(0);
            p.description = cursor.getString(1);
            if (cursor.getString(2) != null) {
                p.setImagePath(cursor.getString(2));
            }
            posts.add(p);
        }
        cursor.close();
    }
    void loadPostsFromSQlite() throws MalformedURLException, ExecutionException, InterruptedException {
        List<Post> postsFromSQL = new ArrayList<>();
        String[] projection = {
                PostsSQLite.PostsEntry.COLUMN_NAME_TITLE,
                PostsSQLite.PostsEntry.COLUMN_NAME_DESCRIPTION,
                PostsSQLite.PostsEntry.COLUMN_NAME_IMAGE_PATH
        };

        Cursor cursor = db.query(PostsSQLite.PostsEntry.TABLE_NAME, projection, null, null, null, null, null);
        while (cursor.moveToNext()) {
            Post p = new Post();
            p.title = cursor.getString(0);
            p.description = cursor.getString(1);
            if (cursor.getString(2) != null) {
                p.setImagePath(cursor.getString(2));
            }
            postsFromSQL.add(0, p);
        }
        cursor.close();
    }


    List collectPosts(Map<String, Object> postsFromDb) {
        List<Post> ps = new ArrayList<>();
        if (postsFromDb != null) {
            for (Map.Entry<String, Object> entry : postsFromDb.entrySet()) {
                Post p = new Post();

                Map singlePost = (Map) entry.getValue();
                String title = singlePost.get("title").toString();
                String description = null;
                String imagePath = null;
                if (singlePost.get("description") != null)
                    description = singlePost.get("description").toString();
                if (singlePost.get("imagePath") != null)
                    imagePath = singlePost.get("imagePath").toString();
                p.title = title;
                p.description = description;
                p.imagePath = imagePath;
                p.setNumber(singlePost.get("number").toString());
                //posts.add(p);
                ps.add(p);
            }
        }
        Collections.sort(ps);
        Collections.reverse(ps);
        return ps;
    }

}
