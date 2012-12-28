/*
 * Copyright (C) 2010 Eric Harlow
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Martin Bruse made some minor changes.
 *
 */

package cx.ath.troja.droidippy;

import java.util.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.util.*;

public class DragAndDropAdapter extends BaseAdapter implements DragAndDropListView.RemoveListener, DragAndDropListView.DropListener {

    private int mId;
    private int mLayout;
    private LayoutInflater mInflater;
    private ArrayList<String> mContent;
    private Context mContext;

    public DragAndDropAdapter(Context context, Collection<String> content) {
        this(context, android.R.layout.simple_list_item_1, android.R.id.text1, content);
    }

    public DragAndDropAdapter(Context context, int itemLayout, int itemID, Collection<String> content) {
        mInflater = LayoutInflater.from(context);
	mContext = context;
        mId = itemID;
        mLayout = itemLayout;
        mContent = new ArrayList<String>(content);
    }

    private void init(Context context, int layout, int[] ids, ArrayList<String> content) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
    }
    
    /**
     * The number of items in the list
     * @see android.widget.ListAdapter#getCount()
     */
    public int getCount() {
        return mContent.size();
    }

    /**
     * Since the data comes from an array, just returning the index is
     * sufficient to get at the data. If we were using a more complex data
     * structure, we would return whatever object represents one row in the
     * list.
     *
     * @see android.widget.ListAdapter#getItem(int)
     */
    public String getItem(int position) {
        return mContent.get(position);
    }

    /**
     * Use the array index as a unique id.
     * @see android.widget.ListAdapter#getItemId(int)
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Make a view to hold each row.
     *
     * @see android.widget.ListAdapter#getView(int, android.view.View,
     *      android.view.ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unneccessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(mLayout, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(mId);

            convertView.setTag(holder);
	    BaseActivity.adjustTextViews(mContext, convertView);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        holder.text.setText(mContent.get(position));
        return convertView;
    }

    static class ViewHolder {
        TextView text;
    }

    public void onRemove(int which) {
	if (which < 0 || which > mContent.size()) return;               
	mContent.remove(which);
	notifyDataSetChanged();
    }

    public void onDrop(int from, int to) {
	String temp = mContent.get(from);
	mContent.remove(from);
	mContent.add(to,temp);
	notifyDataSetChanged();
    }
}