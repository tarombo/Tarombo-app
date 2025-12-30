package app.familygem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.PopupMenu;

import com.squareup.picasso.Picasso;
import com.familygem.utility.GithubUser;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
import app.familygem.R;

public class UsersListViewAdapter extends BaseAdapter {
    // Declare Variables

    Context mContext;
    LayoutInflater inflater;
    private List<GithubUser> userList;

    public interface ItemCallback {
        void onItemClick(GithubUser user);
    }

    private ItemCallback itemCallback;
    private boolean showDeleteMenu;

    public UsersListViewAdapter(Context context, List<GithubUser> githubUserList, ItemCallback itemCallback,
            boolean showDeleteMenu) {
        mContext = context;
        this.userList = githubUserList;
        inflater = LayoutInflater.from(mContext);
        this.itemCallback = itemCallback;
        this.showDeleteMenu = showDeleteMenu;
    }

    public void setUserList(List<GithubUser> githubUserList) {
        this.userList = githubUserList;
    }

    public void removeUser(GithubUser user) {
        for (GithubUser u : userList) {
            if (user.getUserName().equals(u.getUserName())) {
                userList.remove(u);
                break;
            }
        }
    }

    public void clearData() {
        this.userList.clear();
    }

    public class ViewHolder {
        TextView name;
        TextView userName;
        ShapeableImageView avatar;
        ImageButton menu;
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
            holder.menu = view.findViewById(R.id.albero_menu);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        if (!showDeleteMenu) {
            view.setOnClickListener((v) -> {
                itemCallback.onItemClick(userList.get(position));
            });
        }
        // Set the results into TextViews
        GithubUser user = userList.get(position);
        holder.name.setText(user.getName());
        holder.userName.setText(user.getUserName());
        holder.menu.setVisibility(this.showDeleteMenu ? View.VISIBLE : View.GONE);
        holder.menu.setOnClickListener(vista -> {
            PopupMenu popup = new PopupMenu(mContext, vista);
            Menu menu = popup.getMenu();
            menu.add(0, 0, 0, R.string.delete);
            popup.show();
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 0) {
                    // recoverTree(repoFullName, () -> {});
                    itemCallback.onItemClick(userList.get(position));
                } else {
                    return false;
                }
                return true;
            });
        });

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Picasso.get()
                    .load(user.getAvatarUrl())
                    .fit()
                    .centerCrop()
                    .into(holder.avatar);
        } else {
            holder.avatar.setImageResource(R.drawable.no_image);
        }
        return view;
    }
}
