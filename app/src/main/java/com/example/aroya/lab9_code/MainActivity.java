package com.example.aroya.lab9_code;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;


import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<User> userList;
    ArrayList<Integer>userStatus;
    ArrayList<String>userName;
    ArrayList<Repository>repoList;
    ArrayList<Integer>repoStatus;
    GitHubService service;
    int mode,index;
    boolean ServiceUsing;

    private static final int done=0;
    private static final int fail=1;
    private static final int loading=2;
    private static final int user=0;
    private static final int repo=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestNetwork();

        userList=new ArrayList();
        userStatus=new ArrayList();
        repoList=new ArrayList();
        repoStatus=new ArrayList();
        userName=new ArrayList();

        ServiceUsing=true;
        initRetrofit();
        load();
    }

    private void load(){
        setContentView(R.layout.main);
        FreshRecyclerView();

        findViewById(R.id.clear).setOnClickListener(new ClickOnButton());
        findViewById(R.id.fetch).setOnClickListener(new ClickOnButton());

        mode=user;
    }
    private void RepoView(){
        mode=repo;
        setContentView(R.layout.repo);
        FreshRecyclerViewRepo();
    }

    private void FreshRecyclerView(){
        recyclerView=(RecyclerView)findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setAdapter(new MyAdapter());
    }
    private void FreshRecyclerViewRepo(){
        recyclerView=(RecyclerView)findViewById(R.id.repo_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerView.setAdapter(new MyAdapter());
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder>{
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup viewGroup,int viewType){
            View view= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item,viewGroup,false);
            MyViewHolder vh=new MyViewHolder(view);
            return vh;
        }
        @Override
        public void onBindViewHolder(MyViewHolder myViewHolder,int position){
            switch (mode){
                case user:
                    switch (userStatus.get(position)){
                        case done:
                            myViewHolder.progressBar.setVisibility(View.INVISIBLE);
                            myViewHolder.imageView.setVisibility(View.INVISIBLE);
                            myViewHolder.title.setText(userList.get(position).getLogin());
                            myViewHolder.subtitle.setText("id: "+Integer.toString(userList.get(position).getId()));
                            myViewHolder.main.setText("blog: "+userList.get(position).getBlog());
                            break;
                        case fail:
                            myViewHolder.progressBar.setVisibility(View.INVISIBLE);
                            myViewHolder.imageView.setVisibility(View.VISIBLE);
                            myViewHolder.title.setVisibility(View.INVISIBLE);
                            myViewHolder.subtitle.setText("读取失败!");
                            myViewHolder.main.setVisibility(View.INVISIBLE);
                            break;
                        case loading:
                            myViewHolder.imageView.setVisibility(View.INVISIBLE);
                            myViewHolder.progressBar.setVisibility(View.VISIBLE);
                            myViewHolder.title.setVisibility(View.INVISIBLE);
                            myViewHolder.subtitle.setVisibility(View.INVISIBLE);
                            myViewHolder.main.setVisibility(View.INVISIBLE);
                    }
                    break;
                case repo:
                    switch (repoStatus.get(position)){
                        case done:
                            myViewHolder.progressBar.setVisibility(View.INVISIBLE);
                            myViewHolder.imageView.setVisibility(View.INVISIBLE);
                            myViewHolder.title.setText(repoList.get(position).getName());
                            myViewHolder.subtitle.setText(repoList.get(position).getLanguage());
                            myViewHolder.main.setText(repoList.get(position).getDescription());
                            break;
                        case fail:
                            myViewHolder.progressBar.setVisibility(View.INVISIBLE);
                            myViewHolder.imageView.setVisibility(View.VISIBLE);
                            myViewHolder.title.setVisibility(View.INVISIBLE);
                            myViewHolder.subtitle.setText("读取失败!");
                            myViewHolder.main.setVisibility(View.INVISIBLE);
                            break;
                        case loading:
                            myViewHolder.imageView.setVisibility(View.INVISIBLE);
                            myViewHolder.progressBar.setVisibility(View.VISIBLE);
                            myViewHolder.title.setVisibility(View.INVISIBLE);
                            myViewHolder.subtitle.setVisibility(View.INVISIBLE);
                            myViewHolder.main.setVisibility(View.INVISIBLE);
                    }
                    break;
            }

        }
        @Override
        public int getItemCount(){
            switch (mode){
                case user:
                    return userStatus.size();
                case repo:
                    return repoStatus.size();
            }
            return 0;
        }
        public class MyViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener,View.OnLongClickListener{
            TextView title,subtitle,main;
            ProgressBar progressBar;
            ImageView imageView;
            public MyViewHolder(View view){
                super(view);
                title=(TextView)view.findViewById(R.id.item_title);
                subtitle=(TextView)view.findViewById(R.id.item_subtitle);
                main=(TextView)view.findViewById(R.id.item_main);
                progressBar=(ProgressBar)view.findViewById(R.id.item_progress);
                imageView=(ImageView)view.findViewById(R.id.item_fail);

                view.setOnClickListener(this);
                view.setOnLongClickListener(this);
            }

            @Override
            public void onClick(View view){
                if(ServiceUsing){
                    Toast.makeText(getApplicationContext(),
                            "请等待加载完成",
                            Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                switch (mode){
                    case user:
                        repoList.clear();
                        repoStatus.clear();
                        repoList.add(new Repository());
                        repoStatus.add(loading);
                        index=0;
                        RepoView();
                        ServiceUsing=true;
                        service.getRepoList(userName.get(getAdapterPosition()))
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<List<Repository>>() {
                                    @Override
                                    public void onSubscribe(@NonNull Disposable d) {

                                    }

                                    @Override
                                    public void onNext(@NonNull List<Repository> repository) {
                                        repoList.clear();
                                        repoStatus.clear();
                                        for (int i = 0; i < repository.size(); i++) {
                                            repoList.add(repository.get(i));
                                            repoStatus.add(done);
                                        }
                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        repoStatus.set(index, fail);
                                        Toast.makeText(getApplicationContext(),
                                                "获取失败:" + e.toString(), Toast.LENGTH_SHORT)
                                                .show();
                                        FreshRecyclerViewRepo();
                                        ServiceUsing=false;
                                    }

                                    @Override
                                    public void onComplete() {
                                        Toast.makeText(getApplicationContext(),
                                                "获取成功", Toast.LENGTH_SHORT)
                                                .show();
                                        FreshRecyclerViewRepo();
                                        ServiceUsing=false;
                                    }
                                });
                        break;
                }
            }

            @Override
            public boolean onLongClick(View view){
                switch (mode){
                    case user:
                        userList.remove(getAdapterPosition());
                        userStatus.remove(getAdapterPosition());
                        userName.remove(getAdapterPosition());
                        notifyItemRemoved(getAdapterPosition());
                        break;
                }
                return true;
            }
        }
    }
    public interface GitHubService{
        @GET("users/{user}")
        Observable<User> getUser(@Path("user")String user);
        @GET("users/{user}/repos")
        Observable<List<Repository>> getRepoList(@Path("user")String user);
    }

    public class User{
        private int id;
        private String login;
        private String blog;
        public int getId(){return id;}
        public String getLogin(){return login;}
        public String getBlog(){return blog;}
    }
    public class Repository{
        String name;
        String language;
        String description;
        public String getName(){return name;}
        public String getLanguage(){return language;}
        public String getDescription(){return description;}
    }

    public class ClickOnButton implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.clear:
                    ((EditText)findViewById(R.id.input)).setText("");
                    //删除数据
                    userList.clear();
                    userStatus.clear();
                    userName.clear();
                    //更新recyclerView
                    FreshRecyclerView();
                    break;
                case R.id.fetch:
                    if(ServiceUsing){
                        Toast.makeText(getApplicationContext(),
                                "请等待加载完成",
                                Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    index=userStatus.size();
                    userStatus.add(loading);
                    userList.add(new User());
                    FreshRecyclerView();
                    String t=((EditText)findViewById(R.id.input))
                            .getText().toString();
                    userName.add(t);
                    ServiceUsing=true;
                    service.getUser(t)
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<User>() {
                                @Override
                                public void onSubscribe(@NonNull Disposable d) {

                                }

                                @Override
                                public void onNext(@NonNull User user) {
                                    //替换
                                    userList.set(index,user);
                                    //状态改变
                                    userStatus.set(index,done);
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    Toast.makeText(getApplicationContext(),
                                            "获取失败:"+e.toString(),Toast.LENGTH_SHORT)
                                            .show();
                                    userStatus.set(index,fail);
                                    FreshRecyclerView();
                                    ServiceUsing=false;
                                }

                                @Override
                                public void onComplete() {
                                    Toast.makeText(getApplicationContext(),
                                            "获取成功",Toast.LENGTH_SHORT)
                                            .show();
                                    FreshRecyclerView();
                                    ServiceUsing=false;
                                }
                            });
                    break;

            }
        }
    }
    private void requestNetwork(){
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.INTERNET},
                        0);
            }
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String permissons[],int[] grantResults){
        if(grantResults.length>0&&grantResults[0]==
                PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this,"权限申请成功",Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this,"权限申请失败",Toast.LENGTH_SHORT).show();
        }
    }
    private static OkHttpClient createOKHttp(){
        OkHttpClient okHttpClient=new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30,TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .build();
        return okHttpClient;
    }
    private void initRetrofit(){
        Retrofit retrofit=new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(createOKHttp())
                .build();
        service=retrofit.create(GitHubService.class);
        ServiceUsing=false;
    }
    @Override
    public void onBackPressed(){
        switch (mode){
            case repo:
                load();
                break;
            case user:
                finish();
                break;
        }
    }
}
