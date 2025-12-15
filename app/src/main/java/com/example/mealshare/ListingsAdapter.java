package com.example.mealshare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.util.List;

public class ListingsAdapter extends RecyclerView.Adapter<ListingsAdapter.ViewHolder> {

    private final Context context;
    private final List<Listing> listings;

    public ListingsAdapter(Context context, List<Listing> listings) {
        this.context = context;
        this.listings = listings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Listing listing = listings.get(position);
        holder.name.setText(listing.getFood());
        holder.quantity.setText(String.valueOf(listing.getQuantity()));
        holder.location.setText(listing.getLocation());

        if (listing.getPicture() != null && !listing.getPicture().isEmpty()) {
            Glide.with(context)
                    .asBitmap()
                    .load(listing.getPicture())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            holder.image.setImageBitmap(resource);
                            Palette.from(resource).generate(palette -> {
                                if (palette != null) {
                                    int vibrantColor = palette.getVibrantColor(0x000000); // Default to black
                                    holder.listingLayout.setBackgroundColor(vibrantColor);
                                }
                            });
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name;
        TextView quantity;
        TextView location;
        Button detailsButton;
        LinearLayout listingLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.listingImageView);
            name = itemView.findViewById(R.id.listingNameTextView);
            quantity = itemView.findViewById(R.id.listingQuantityTextView);
            location = itemView.findViewById(R.id.listingLocationTextView);
            detailsButton = itemView.findViewById(R.id.detailsButton);
            listingLayout = itemView.findViewById(R.id.listingLayout);
        }
    }
}
