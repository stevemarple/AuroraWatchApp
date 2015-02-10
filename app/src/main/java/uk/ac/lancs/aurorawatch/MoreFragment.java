package uk.ac.lancs.aurorawatch;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

/**
 * Created by jamesb on 04/02/2015.
 */
public class MoreFragment extends Fragment {

    ImageButton twitterButton;
    ImageButton facebookButton;
    ImageButton flickrButton;
    Button faqButton;
    Button websiteButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.more, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        twitterButton = (ImageButton)getView().findViewById(R.id.twitterButton);
        facebookButton = (ImageButton)getView().findViewById(R.id.facebookButton);
        flickrButton = (ImageButton)getView().findViewById(R.id.flickrButton);
        faqButton = (Button)getView().findViewById(R.id.faqButton);
        websiteButton = (Button)getView().findViewById(R.id.awUrlButton);

        //TODO: Refactor the below to one OnClickListner and switch depending on calling view?
        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://twitter.com/aurorawatchuk");
            }
        });
        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://www.facebook.com/aurorawatchuk");
            }
        });
        flickrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("http://www.flickr.com/groups/aurorawatch");
            }
        });
        websiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("http://aurorawatch.lancs.ac.uk/");
            }
        });
    }

    private void openUrl(String url) {
        Uri destination = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, destination);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }
}