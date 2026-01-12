package com.example.jemaahapps;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScanGroupedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ScanRow> rows;

    public ScanGroupedAdapter(List<ScanRow> rows) {
        this.rows = rows;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == ScanRow.TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_program_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_scan_person, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ScanRow row = rows.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).tvProgram.setText(row.getProgramName());
        } else if (holder instanceof ItemVH) {
            ItemVH itemVH = (ItemVH) holder;

            // find index within its program group
            int number = 1;
            for (int i = position - 1; i >= 0; i--) {
                ScanRow prev = rows.get(i);
                if (prev.getType() == ScanRow.TYPE_HEADER) {
                    break;                  // stop at header
                }
                number++;                   // count previous items in same group
            }

            itemVH.tvNumber.setText(number + ".");
            itemVH.tvName.setText(row.getName());
            itemVH.tvPhone.setText(row.getPhone());
        }
    }


    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvProgram;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvProgram = itemView.findViewById(R.id.tvProgramHeader);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {
        TextView tvNumber, tvName, tvPhone;
        ItemVH(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tvNumber);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
        }
    }

}
