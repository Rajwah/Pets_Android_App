package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * Created by rajwahalqurashi on 2018-05-08.
 */

public class PetProvider  extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();



    private static final int PETS=100;
    private static final int PETS_ID=101;


    private static final UriMatcher sUriMatcher =  new UriMatcher(UriMatcher.NO_MATCH);

    static{
        //ACT on the entire pets table
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        //Act on a specific row with id 101
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS+ "/#", PETS_ID);

    }

    //database helper object
    private PetDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        // TODO: Create and initialize a PetDbHelper object to gain access to the pets database.
        // Make sure the variable is a global variable, so it can be referenced from other
        // ContentProvider methods.
        mDbHelper= new PetDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;

        int match= sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                cursor= database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

                break;
            case PETS_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query"+ uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;

    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
       final int match = sUriMatcher.match(uri);
       switch (match){
           case PETS:
               return insertPet(uri, contentValues);
           default:
               throw new IllegalArgumentException("Insertion is not supported for"+ uri);
       }
    }

    private Uri insertPet(Uri uri, ContentValues values){
        String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        if(name==null){
            throw new IllegalArgumentException("Pet requires a name");}

        Integer gender= values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
        if(gender == null || !PetContract.PetEntry.isValidGender(gender)){
            throw new IllegalArgumentException("Pet requires valid gender");}

            Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
        if(weight!=null && weight<0){
            throw new IllegalArgumentException("Pet requires valid weight");
        }
        //get writeble database
        SQLiteDatabase database= mDbHelper.getWritableDatabase();
        //insert the new pet with the given values
        long id = database.insert(PetContract.PetEntry.TABLE_NAME , null, values);
        //if the id is -1 then the insertion failed. Log an error ad return null
        if(id==-1) {
            Log.e(LOG_TAG, "Failed to insert row for" + uri);
            return null;
        }
        //notify all listeners that the data has changed for the pet content URI
        getContext().getContentResolver().notifyChange(uri, null);
        //return the new URI with the ID (OF THE newly INSERTED RWO) append at the end.
        return ContentUris.withAppendedId(uri, id);

    }
    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
     final int match = sUriMatcher.match(uri);
     switch (match){
         case PETS:
             return updatePet(uri, contentValues, selection, selectionArgs);
         case PETS_ID:
             selection = PetContract.PetEntry._ID + "=?";
             selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
             return updatePet(uri, contentValues, selection, selectionArgs);
         default:
             throw new IllegalArgumentException("Update is not supported for"+ uri);
     }
    }

    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }
        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_NAME);
            if (gender == null || !PetContract.PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet requires a valid gender");
            }
        }

        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_WEIGHT)) {
            Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_NAME);
            if (weight != null || weight < 0) {
                throw new IllegalArgumentException("Pet requires a valid weight");
            }
        }
        //if there is no values to update
        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        //return the number of database rows affected by the update statement
        int rowsUpdated = database.update(PetContract.PetEntry.TABLE_NAME, values, selection, selectionArgs);
        //if 1 or more rwos are updated then notify all listeners that data at given URI is updated
        if(rowsUpdated !=0){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }


    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted;
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                rowsDeleted= database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PETS_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted= database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for"+ uri);
        }
        if(rowsDeleted !=0){
            getContext().getContentResolver().notifyChange(uri, null);
        }
return rowsDeleted;
    }

    /**
     * Returns the MIME type of data for the content URI.
     */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PETS_ID:
            return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI"+ uri +" with match"+ match);
        }
    }
}
