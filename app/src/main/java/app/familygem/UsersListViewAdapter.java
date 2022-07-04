package app.familygem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.Function;
import kotlin.Unit;

public class UsersListViewAdapter extends BaseAdapter {
    // Declare Variables

    Context mContext;
    LayoutInflater inflater;
    private List<GithubUser> userList = null;
    private ArrayList<GithubUser> arraylist;

    public interface ItemCallback {
        void onItemClick(GithubUser user);
    }

    private ItemCallback itemCallback;

    public UsersListViewAdapter(Context context, List<GithubUser> GithubUserList, ItemCallback itemCallback) {
        mContext = context;
        this.userList = GithubUserList;
        inflater = LayoutInflater.from(mContext);
        this.arraylist = new ArrayList<>();
        this.arraylist.addAll(GithubUserList);
        this.itemCallback = itemCallback;
    }

    public class ViewHolder {
        TextView name;
    }

    @Override
    public int getCount() {
        return userList.size();
    }

    @Override
    public GithubUser getItem(int position) {
        return userList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.user_list_view_item, null);
            // Locate the TextViews in listview_item.xml
            holder.name = (TextView) view.findViewById(R.id.name);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        view.setOnClickListener((v) -> {
            itemCallback.onItemClick(arraylist.get(position));
        });
        // Set the results into TextViews
        holder.name.setText(userList.get(position).getName());
        return view;
    }

    // Filter Class
    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        userList.clear();
        if (charText.length() == 0) {
            userList.addAll(arraylist);
        } else {
            for (GithubUser wp : arraylist) {
                if (wp.getName().toLowerCase(Locale.getDefault()).contains(charText)) {
                    userList.add(wp);
                }
            }
        }
        notifyDataSetChanged();
    }
}
