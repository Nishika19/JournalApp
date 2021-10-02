package com.example.journal.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.journal.R;
import com.example.journal.model.Journal;
import com.squareup.picasso.Picasso;

import java.util.List;

import util.JournalApi;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private onCardClickListener cardClickListener;
    private Context context;
    private List<Journal> journalList;

    public RecyclerViewAdapter(Context context, List<Journal> journalList, onCardClickListener cardClickListener) {
        this.context = context;
        this.journalList = journalList;
        this.cardClickListener = cardClickListener;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.journal_row,parent,false);
        return new ViewHolder(view,context,cardClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
        Journal journal = journalList.get(position);

        holder.name.setText(journal.getName());
        holder.about.setText(journal.getAbout());
        holder.username_onTop.setText(JournalApi.getInstance().getUsername());

        String imageUrl;

        imageUrl = journal.getImageUrl();

        //date format
        String timeAgo = (String) DateUtils.getRelativeTimeSpanString(journal.getTimeAdded().getSeconds()*1000);
        holder.dateCreated.setText(timeAgo);
        //image
        Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.download)
                .fit()
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        onCardClickListener cardClickListener;
        public TextView name, about, dateCreated,username_onTop;
        public ImageView imageView,shareButton;
        String userId,username;
        public ViewHolder(@NonNull View itemView, Context context,onCardClickListener cardClickListener) {
            super(itemView);
            context = context;

            name = itemView.findViewById(R.id.journal_name_list);
            about = itemView.findViewById(R.id.journal_about_list);
            dateCreated = itemView.findViewById(R.id.journal_timestamp_list);
            imageView = itemView.findViewById(R.id.journal_image_list);
            username_onTop = itemView.findViewById(R.id.journal_row_username);
            shareButton = itemView.findViewById(R.id.journal_row_share_button);
            this.cardClickListener = cardClickListener;

            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
                cardClickListener.onCardClick(getAdapterPosition());
                return true;
        }
    }

    public interface onCardClickListener{
        void onCardClick(int position);
    }
}
