package com.example.decisionmaker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private ArrayList<String> items;
    private ItemsAdapter adapter;
    private RecyclerView itemsRv;
    private TextView resultTextView;
    private EditText newItemEditText;
    private ItemTouchHelper itemTouchHelper;
    private Button categoryButton;
    private TextView addCategoryTextView;
    private TextView removeCategoryTextView;
    private Button addButton;

    private HashMap<String, ArrayList<String>> categoryHash = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        itemsRv = findViewById(R.id.itemsRv);
        resultTextView = findViewById(R.id.resultTextView);
        newItemEditText = findViewById(R.id.newItemEditText);
        categoryButton = findViewById(R.id.categoryButton);
        addButton = findViewById(R.id.addButton);
        addCategoryTextView = findViewById(R.id.addCategoryTextView);
        addCategoryTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCategory();
            }
        });
        removeCategoryTextView = findViewById(R.id.removeCategoryTextView);
        removeCategoryTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCategory();
            }
        });

        load();

        // for swipe to delete
        itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(adapter));
        itemTouchHelper.attachToRecyclerView(itemsRv);
    }

    /**
     * Function to randomly choose an item from the displayed category
     * @param view
     */
    public void onChoose(View view) {
        Random random = new Random();
        if (items.isEmpty()) {
            Toast.makeText(this, "There is nothing to choose from!", Toast.LENGTH_SHORT).show();
            return;
        }
        String chosen = items.get(random.nextInt(items.size()));
        resultTextView.setText(chosen);
    }

    /**
     * Function to handle adding new items to the displayed category
     * New items are inserted at the top of the list
     * @param view
     */
    public void onAdd(View view) {
        String toAdd = newItemEditText.getText().toString();
        if (toAdd.isEmpty()) {
            Toast.makeText(this, "Please enter an item", Toast.LENGTH_SHORT).show();
            return;
        }
        items.add(0, toAdd);
        adapter.notifyItemInserted(0);
        itemsRv.scrollToPosition(0);
        newItemEditText.setText("");
    }

    /**
     * Function to handle displaying categories
     * The popup menu is created dynamically everytime it is pressed
     * TODO: Might be possible to initialise the popup once instead of re-creating it everytime
     * @param view
     */
    public void showPopup(View view) {
        if (categoryHash.isEmpty()) return;
        PopupMenu popup = new PopupMenu(this, view);
        for (String s : categoryHash.keySet()) { // add categories to popup menu
            popup.getMenu().add(s);
        }
        popup.inflate(R.menu.category_menu);
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item != null) {
            switchAdapterDataset(item.toString());
            return true;
        }
        return false;
    }

    /**
     * Function to handle switching of categories.
     * Displays the correct list items when categories are switched, and also updates the category button to show the correct category.
     * @param category
     */
    private void switchAdapterDataset(String category) {
        items = categoryHash.get(category);
        if (items == null) {
            items = new ArrayList<>();
        }
        // handle switching of adapters and assigning swipe to delete to the new adapter
        categoryButton.setText(category);
        adapter = new ItemsAdapter(items);
        itemTouchHelper.attachToRecyclerView(null);
        itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(adapter));
        itemTouchHelper.attachToRecyclerView(itemsRv);
        itemsRv.swapAdapter(adapter, false);
    }

    /**
     * Function to handle adding of new categories
     */
    public void addCategory() {
        // get the layout to be displayed in the dialog
        // technically don't need a separate view in this case (can just create an EditText programmatically)
        // but for future references sake I inflated a layout
        final View view = getLayoutInflater().inflate(R.layout.fragment_new_category, null);
        final EditText editText = view.findViewById(R.id.newCategoryEditText); // obtain the EditText from the inflated layout

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Add new category");

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newCategory = editText.getText().toString();
                Log.v("Added Category", newCategory);
                if (newCategory.isEmpty()) { // nothing entered
                    Toast.makeText(MainActivity.this, "Please enter a category", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (categoryHash.get(newCategory) != null) { // category already exists
                    Toast.makeText(MainActivity.this, newCategory + " already exists!", Toast.LENGTH_SHORT).show();
                    return;
                }
                categoryHash.put(newCategory, new ArrayList<>());
                if (!addButton.isEnabled()) addButton.setEnabled(true);
                if (removeCategoryTextView.getVisibility() == View.INVISIBLE) removeCategoryTextView.setVisibility(View.VISIBLE);
                if (itemsRv.getAdapter() == null) setUpAdapter(); // for the situation when we just added the first category
                switchAdapterDataset(newCategory); // switch to the new category's list for allow easy adding of items
                newItemEditText.requestFocus(); // switch to the add item input for better ease of use
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        alertDialog.setView(view);
        alertDialog.show();
    }

    /**
     * Function to handle the removal of a category
     */
    public void removeCategory() {
        String categoryToRemove = categoryButton.getText().toString();

        // show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete " + categoryToRemove + "?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        categoryHash.remove(categoryToRemove);
                        if (categoryHash.size() > 0) { // switch to first category in list
                            for (String category : categoryHash.keySet()) {
                                switchAdapterDataset(category);
                                break;
                            }
                        } else { // switch to empty array and remove all items from recyclerview
                            switchAdapterDataset("");
                            addButton.setEnabled(false);
                            categoryButton.setText(R.string.noCategoryText);
                            removeCategoryTextView.setVisibility(View.INVISIBLE);
                        }
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void save() {
        SharedPreferences prefs = getSharedPreferences("saveData", Context.MODE_PRIVATE);
        if (prefs != null) {
            JSONObject jsonObject = new JSONObject(categoryHash);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("categoryHash").commit();
            editor.putString("categoryHash", jsonString).commit();
        }
        Log.v("Save", "Saving");
    }

    private void load() {
        categoryHash = new HashMap<>();
        SharedPreferences prefs = getSharedPreferences("saveData", Context.MODE_PRIVATE);
        try {
            if (prefs != null) {
                String jsonString = prefs.getString("categoryHash", (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    JSONArray value = (JSONArray) jsonObject.get(key);
                    if (value != null) { // because i cannot get the ArrayList directly from the JSONObject
                        ArrayList<String> list = new ArrayList<>();
                        for (int i=0; i<value.length(); i++) {
                            list.add(value.getString(i));
                        }
                        categoryHash.put(key, list);
                    }
                }
                if (categoryHash.isEmpty()) { // no previously saved categories
                    items = new ArrayList<>();
                    categoryButton.setText(R.string.noCategoryText);
                    addButton.setEnabled(false);
                    removeCategoryTextView.setVisibility(View.INVISIBLE);
                } else {
                    for (String key : categoryHash.keySet()) { // display the first category in list
                        items = categoryHash.get(key);
                        categoryButton.setText(key);
                        break;
                    }
                    setUpAdapter();
                }

            } else {
                Log.v("load failed", "prefs is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpAdapter() {
        itemsRv.requestFocus(); // had to do this if not keyboard would not work after loading for reasons unknown
        adapter = new ItemsAdapter(items);
        itemsRv.setAdapter(adapter);
        itemsRv.setLayoutManager(new LinearLayoutManager(this));
        itemsRv.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL)); // add lines between items in recyclerview
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        save();
    }
}