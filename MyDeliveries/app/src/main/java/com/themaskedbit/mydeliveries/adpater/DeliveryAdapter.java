package com.themaskedbit.mydeliveries.adpater;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.themaskedbit.mydeliveries.R;
import com.themaskedbit.mydeliveries.model.Delivery;


import java.util.List;

public class DeliveryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Delivery> deliveries;
    private Context context;
    private DeliveryViewClickListener mListener;

    public DeliveryAdapter(RecyclerView recyclerView, List<Delivery> deliveries, Context context, DeliveryViewClickListener mListener) {

        this.deliveries = deliveries;
        this.context = context;
        this.mListener = mListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        System.out.println("viewType : "+ viewType);
        Log.e("DeliveryAdapter",viewType+"");
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_delivery, parent, false);
        return new DeliveryViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            DeliveryViewHolder dHolder = (DeliveryViewHolder) holder;
            String image_url = deliveries.get(position).getImageUrl();
            Picasso.with(context)
                    .load(image_url)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .into(dHolder.deliveryImage);
            String message = deliveries.get(position).getDescription() + R.string.at_string +deliveries.get(position).getLocation().getAddress();
            dHolder.deliveryDescription.setText(message);
    }

    @Override
    public int getItemCount() {
        return deliveries==null ? 0 : deliveries.size();
    }

    public class DeliveryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout deliveryLayout;
        TextView deliveryDescription;
        ImageView deliveryImage;
        DeliveryViewClickListener mListener;

        public DeliveryViewHolder(View itemView, DeliveryViewClickListener mListener) {
            super(itemView);
            deliveryLayout = (LinearLayout)itemView.findViewById(R.id.delivery_layout);
            deliveryDescription = (TextView)itemView.findViewById(R.id.delivery_description);
            deliveryImage = (ImageView)itemView.findViewById(R.id.delivery_image);
            this.mListener = mListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mListener.onClick(view,getAdapterPosition());
        }
    }
    public interface DeliveryViewClickListener {
        void onClick(View view, int position);
    }
}
