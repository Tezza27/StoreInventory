package com.example.android.storeinventory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.storeinventory.data.ProductContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String LOG_TAG = EditorActivity.class.getSimpleName();

    // The request code to store image from the Gallery
    private static final int PICK_IMAGE_REQUEST = 0;

    /** Identifier for the product data loader */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    /** Content URI for the existing product (null if it's a new product) */
    private Uri mCurrentProductUri;

    /** URI for the product image */
    private Uri mImageUri;

    /** EditText field to enter the product's name */
    private EditText mNameEditText;

    /** EditText field to enter the product's description */
    private EditText mDescriptionEditText;

    /** ImageView to enter the product's image uri */
    private ImageView mImageImageView;

    /** EditText field to enter the product's price */
    private EditText mPriceEditText;

     /** EditText field to enter the product's current stock quantity */
    private TextView mStockTextView;

    /** EditText field to enter the change in product's stock quantity */
    private EditText mStockChangeEditText;

    /** EditText field to enter the product's supplier */
    private EditText mSupplierEditText;

    /** EditText field to enter the supplier's email address */
    private EditText mEmailEditText;

    /** EditText field to enter the supplier's email address */
    private EditText mOrderQtyEditText;

    /** Button to reduce stock holding */
    private Button mReduceStockButton;

    /** Button to increase stock holding */
    private Button mIncreaseStockButton;

    /** Button to send order by email */
    private Button mPlaceOrderButton;

    /** Button to delete product */
    private Button mDeleteProductButton;

    /** Boolean flag that keeps track of whether the product has been edited (true) or not (false) */
    private boolean mProductHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle(getString(R.string.editor_activity_title_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();

            //mStockChangeEditText.setVisibility(View.GONE);


        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(getString(R.string.editor_activity_title_edit_product));

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mDescriptionEditText = (EditText) findViewById(R.id.edit_description);
        mImageImageView = (ImageView) findViewById(R.id.edit_image);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mSupplierEditText = (EditText) findViewById(R.id.edit_supplier);
        mEmailEditText = (EditText) findViewById(R.id.edit_email);
        mStockTextView = (TextView) findViewById(R.id.edit_stockholding);
        mStockChangeEditText = (EditText) findViewById(R.id.edit_stock_change);
        mOrderQtyEditText = (EditText) findViewById(R.id.edit_order_qty);
        mReduceStockButton = (Button) findViewById(R.id.edit_stock_minus_button);
        mIncreaseStockButton = (Button) findViewById(R.id.edit_stock_plus_button);
        mPlaceOrderButton = (Button) findViewById(R.id.edit_order_button);
        mDeleteProductButton = (Button) findViewById(R.id.edit_delete_button);


        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mDescriptionEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mImageImageView.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mEmailEditText.setOnTouchListener(mTouchListener);
        mStockTextView.setOnTouchListener(mTouchListener);
        mStockChangeEditText.setOnTouchListener(mTouchListener);
        mDeleteProductButton.setOnTouchListener(mTouchListener);

        // Set a clickListener on minus button
        mReduceStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentProduct =  mNameEditText.getText().toString();
                String toastMessage = null;
                int currentStock = parseInt(mStockTextView.getText().toString());
                int changeStock;
                int newStockholding;

                if (mStockChangeEditText.getText().toString().trim().length() == 0) {
                    newStockholding = currentStock - 1;
                } else {
                    changeStock = parseInt(mStockChangeEditText.getText().toString());
                    newStockholding = currentStock - changeStock;
                }

                if (newStockholding == 0) {
                    toastMessage = "You are out of stock of " + currentProduct + ".  Place an order";
                    mStockTextView.setText(String.valueOf(newStockholding));
                } else if (newStockholding >0) {
                    toastMessage = "Stock of " + currentProduct + " has reduced from " + currentStock + " to " + newStockholding;
                    mStockTextView.setText(String.valueOf(newStockholding));
                } else {
                    toastMessage = "You can't reduce your stock of " + currentProduct + " by more than your current stockholding";
                }

                Toast.makeText(view.getContext(), toastMessage, Toast.LENGTH_LONG).show();
                mStockChangeEditText.setText((""));
                }
        });

        // Set a clickListener on plus button
        mIncreaseStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentProduct =  mNameEditText.getText().toString();
                String toastMessage = null;
                int currentStock = parseInt(mStockTextView.getText().toString());
                int changeStock;
                int newStockholding;

                if (mStockChangeEditText.getText().toString().trim().length() == 0) {
                    newStockholding = currentStock + 1;
                } else {
                    changeStock = parseInt(mStockChangeEditText.getText().toString());
                    newStockholding = currentStock + changeStock;
                }

                    mStockTextView.setText(String.valueOf(newStockholding));
                    toastMessage = "Your stock of " + currentProduct + " has increased from " + currentStock + " to " + newStockholding;

                Toast.makeText(view.getContext(), toastMessage, Toast.LENGTH_LONG).show();
                mStockChangeEditText.setText((""));
            }
        });

        // Set a clickListener on email button
        mPlaceOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String orderQty = mOrderQtyEditText.getText().toString().trim();
                mOrderQtyEditText.setText("");
                if (orderQty.length() != 0) {
                    String productName = mNameEditText.getText().toString().trim();

                    String emailAddress = "mailto:" + mEmailEditText.getText().toString().trim();
                    String subjectHeader = "Order For: " + productName;
                    String orderMessage = "Please send " + orderQty + " units of " + productName + ". " + " \n\n" + "Thank you.";

                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse(emailAddress));
                    intent.putExtra(Intent.EXTRA_SUBJECT, subjectHeader);
                    intent.putExtra(Intent.EXTRA_TEXT, orderMessage);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }

                   } else {
                    String toastMessage = "Order quantity required";
                    Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
                }

            }
        });

        // Set a clickListener on delete button
        mDeleteProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
            }
        });

        mImageImageView.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View view) {
                openImageSelector();
                                          }
    });
}

    /**
     * Get user input from editor and save product into database.
     */
    private boolean saveProduct() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String descriptionString = mDescriptionEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String stockString =  mStockTextView.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String emailString = mEmailEditText.getText().toString().trim();


        // Check if this is supposed to be a new product
        // and check if all the fields in the editor are blank
        if (mCurrentProductUri == null && mImageUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(descriptionString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(supplierString) && TextUtils.isEmpty(emailString)) {
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return true;
        } else if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, R.string.no_name_toast,
                    Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(descriptionString)) {
            Toast.makeText(this, R.string.no_description_toast,
                    Toast.LENGTH_SHORT).show();
            return false;

        } else if (TextUtils.isEmpty(priceString) || parseDouble(priceString) <= 0) {
            Toast.makeText(this, R.string.no_price_toast,
                    Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(stockString) || parseInt(stockString) < 0) {
            Toast.makeText(this, R.string.no_stock_toast,
                    Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(supplierString)) {
            Toast.makeText(this, R.string.no_supplier_toast,
                    Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(emailString)) {
            Toast.makeText(this, R.string.no_email_toast,
                    Toast.LENGTH_SHORT).show();
            return false;
        } else if (mImageUri == null) {
            Toast.makeText(this, R.string.no_image_toast,
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, descriptionString);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceString);
        values.put(ProductEntry.COLUMN_PRODUCT_STOCK, stockString);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplierString);
        values.put(ProductEntry.COLUMN_SUPPLIER_EMAIL, emailString);

        if (mImageUri == null) {
            mImageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                    "://" + getResources().getResourcePackageName(R.drawable.no_image)
                    + '/' + getResources().getResourceTypeName(R.drawable.no_image) + '/' + getResources().getResourceEntryName(R.drawable.no_image));
        }
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, mImageUri.toString());

        // Determine if this is a new or existing product by checking if mCurrentProductUri is null or not
        if (mCurrentProductUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentProductUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();

            }
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                saveProduct();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_SUPPLIER_EMAIL,
                ProductEntry.COLUMN_PRODUCT_STOCK,};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,     // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int descriptionColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);
            int emailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_SUPPLIER_EMAIL);
            int stockColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_STOCK);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String description = cursor.getString(descriptionColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            String image = cursor.getString(imageColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            String email = cursor.getString(emailColumnIndex);
            int stock = cursor.getInt(stockColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mDescriptionEditText.setText(description);
            mPriceEditText.setText(Double.toString(price));
            mSupplierEditText.setText(supplier);
            mEmailEditText.setText(email);
            mStockTextView.setText(stock);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mDescriptionEditText.setText("");
        mPriceEditText.setText("");
        mSupplierEditText.setText("");
        mEmailEditText.setText("");
        mStockTextView.setText(0);
        mStockChangeEditText.setText("1");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    public void openImageSelector() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mImageUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mImageUri.toString());

                //mImageView.setText(mUri.toString());
                mImageImageView.setImageBitmap(getBitmapFromUri(mImageUri));
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = mImageImageView.getWidth();
        int targetH = mImageImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }
}