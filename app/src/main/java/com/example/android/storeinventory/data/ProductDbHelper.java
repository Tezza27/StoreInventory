package com.example.android.storeinventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.storeinventory.data.ProductContract.ProductEntry;

/**
 * Database helper for Inventory app. Manages database creation and version management.
 */
public class ProductDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = ProductDbHelper.class.getSimpleName();

    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "inventory.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructs a new instance of {@link ProductDbHelper}.
     *
     * @param context of the app
     */
    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the products table
        String SQL_CREATE_PRODUCT_TABLE = "CREATE TABLE " + ProductEntry.TABLE_NAME + " ("
                + ProductEntry.PRODUCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_DESCRIPTION + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_PRICE + " FLOAT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_STOCK + " INTEGER NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_IMAGE + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_SUPPLIER + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_SUPPLIER_EMAIL + " TEXT NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_PRODUCT_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The database is still at version 1, so there's nothing to do be done here.
    }
}
