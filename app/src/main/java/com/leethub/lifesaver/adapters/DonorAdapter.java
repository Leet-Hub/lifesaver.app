package com.leethub.lifesaver.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.leethub.lifesaver.R;
import com.leethub.lifesaver.models.User;

import java.util.List;

public class DonorAdapter extends RecyclerView.Adapter<DonorAdapter.DonorViewHolder> {

    private final Context context;
    private final List<User> donorList;
    private final OnDonorClickListener clickListener;

    public interface OnDonorClickListener {
        void onViewDetailsClicked(User user);
        void onRequestDonateClicked(User user);
    }

    public DonorAdapter(Context context, List<User> donorList, OnDonorClickListener clickListener) {
        this.context = context;
        this.donorList = donorList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public DonorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_donor_card, parent, false);
        return new DonorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonorViewHolder holder, int position) {
        User donor = donorList.get(position);

        holder.tvDonorName.setText(donor.name);
        holder.tvDonorLocation.setText(donor.city != null ? donor.city : "Unknown Location");
        holder.tvDonorBloodGroup.setText(donor.bloodGroup);

        // Optional: Load profile image using Glide/Picasso if the user object had a photoUrl
        // For now, it defaults to the sym_def_app_icon in XML.

        holder.btnViewDetails.setOnClickListener(v -> clickListener.onViewDetailsClicked(donor));
        holder.btnRequestDonate.setOnClickListener(v -> clickListener.onRequestDonateClicked(donor));
    }

    @Override
    public int getItemCount() {
        return donorList.size();
    }

    public static class DonorViewHolder extends RecyclerView.ViewHolder {
        TextView tvDonorName, tvDonorLocation, tvDonorBloodGroup;
        // No action icons anymore
        Button btnViewDetails, btnRequestDonate;

        public DonorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDonorName = itemView.findViewById(R.id.tvDonorName);
            tvDonorLocation = itemView.findViewById(R.id.tvDonorLocation);
            tvDonorBloodGroup = itemView.findViewById(R.id.tvDonorBloodGroup);


            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnRequestDonate = itemView.findViewById(R.id.btnRequestDonate);
        }
    }
}
