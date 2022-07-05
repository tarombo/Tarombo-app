package app.familygem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.familygem.utility.GithubUser;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class UsersListViewAdapter extends BaseAdapter {
    // Declare Variables

    Context mContext;
    LayoutInflater inflater;
    private List<GithubUser> userList;

    public interface ItemCallback {
        void onItemClick(GithubUser user);
    }

    private ItemCallback itemCallback;

    public UsersListViewAdapter(Context context, List<GithubUser> githubUserList, ItemCallback itemCallback) {
        mContext = context;
        this.userList = githubUserList;
        inflater = LayoutInflater.from(mContext);
        this.itemCallback = itemCallback;
    }

    public void setUserList(List<GithubUser> githubUserList) {
        this.userList = githubUserList;
    }

    public void clearData() {
        this.userList.clear();
    }

    public class ViewHolder {
        TextView name;
        TextView userName;
        ShapeableImageView avatar;
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
            holder.userName = (TextView) view.findViewById(R.id.username);
            holder.avatar = (ShapeableImageView) view.findViewById(R.id.avatar);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        view.setOnClickListener((v) -> {
            itemCallback.onItemClick(userList.get(position));
        });
        // Set the results into TextViews
        GithubUser user = userList.get(position);
        holder.name.setText(user.getName());
        holder.userName.setText(user.getUserName());

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(mContext)
                    .load(user.getAvatarUrl())
                    .centerCrop()
                    .into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.no_image);
        }
        return view;
    }
}
