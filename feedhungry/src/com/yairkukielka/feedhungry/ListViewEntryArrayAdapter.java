/**
 * Copyright 2013 Ognyan Bankov
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
 */

package com.yairkukielka.feedhungry;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.yairkukielka.feedhungry.feedly.ListEntry;
import com.yairkukielka.feedhungry.toolbox.DateUtils;


public class ListViewEntryArrayAdapter extends ArrayAdapter<ListEntry> {
	private static final String HTML_OPEN_MARK = "<";
    private ImageLoader mImageLoader;
    private Context context;
    Animation animation;
    
    public ListViewEntryArrayAdapter(Context context, 
                              int textViewResourceId, 
                              List<ListEntry> objects,
                              ImageLoader imageLoader) {
        super(context, textViewResourceId, objects);
        mImageLoader = imageLoader;
        this.context = context;
		animation = AnimationUtils.loadAnimation(context, R.anim.wave_scale);
    }

    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        // only animate the first time they're created
        boolean animate = false;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.feed_list_row, null);
            animate = true;            
        }
        
        ViewHolder holder = (ViewHolder) v.getTag(R.id.id_holder);       
        
        if (holder == null) {
            holder = new ViewHolder(v);
            v.setTag(R.id.id_holder, holder);
        }        
        
        ListEntry entry = getItem(position);
        if (entry.getVisual() != null) {
            holder.image.setImageUrl(entry.getVisual(), mImageLoader);
        } else {
        	//ViewGroup.LayoutParams lp = holder.image.getLayoutParams();
        	holder.image.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        	
            //holder.image.setImageResource(R.drawable.no_image_transp);
        }
        
        String summary = getSummaryWithoutHTML(entry.getContent());
        //Spanned summary = Html.fromHtml(entry.getContent());
//        SpannableStringBuilder spanstr = new SpannableStringBuilder(entry.getTitle());
//        spanstr.setSpan(new StyleSpan(Typeface.BOLD),0, entry.getTitle().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);        
//        spanstr.append(" ");
//        spanstr.append(summary)
//        holder.title.setText(spanstr);;

        holder.title.setText(entry.getTitle());
        holder.summary.setText(summary);
        holder.date.setText(DateUtils.dateToString(entry.getPublished()));
        
        
        if (v != null && animate) {
			animation.setDuration(500);
			v.startAnimation(animation);
        }
        return v;
    }
    
    
    private class ViewHolder {
        NetworkImageView image;
        TextView title; 
        TextView summary; 
        TextView date; 
        
        public ViewHolder(View v) {
            image = (NetworkImageView) v.findViewById(R.id.image_list_thumb);
            title = (TextView) v.findViewById(R.id.tv_list_title);
            summary = (TextView) v.findViewById(R.id.tv_list_summary);
            date = (TextView) v.findViewById(R.id.tv_list_date);
            
            v.setTag(this);
        }
    }
    
    private String getSummaryWithoutHTML(String s) {
    	if (!(s == null || "".equals(s) || s.startsWith(HTML_OPEN_MARK))) {
    		int index = s.indexOf(HTML_OPEN_MARK);
    		if (index != -1) {
    			return s.substring(0, index);
    		}
    	}
		return "";
    }
}
