package com.beechat.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class DatabaseHandler extends SQLiteOpenHelper {
    private static final String DATABASE_PATH = "//data/data/com.beechat.network/databases/";
    private static final String DATABASE_NAME = "XBEE.DB";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USER = "user";
    private static final String TABLE_MESSAGE = "message";
    private static final String TABLE_DEVICE = "device";

    private static final String USER_ID = "id";
    private static final String USER_XBEE_DEVICE_NUMBER = "xbee_device_number";
    private static final String USER_NAME = "name";

    private static final String MESSAGE_ID = "id";
    private static final String MESSAGE_SENDER_ID = "senderId";
    private static final String MESSAGE_RECEIVER_ID = "receiverId";
    private static final String MESSAGE_CONTENT = "content";

    private static final String DEVICE_ID = "id";
    private static final String DEVICE_XBEE_DEVICE_NUMBER = "xbee_device_number";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //3rd argument to be passed is CursorFactory instance
    }


    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + "("
                + USER_ID + " INTEGER PRIMARY KEY," + USER_XBEE_DEVICE_NUMBER + " TEXT," + USER_NAME + " TEXT" + ")";

        String CREATE_MESSAGE_TABLE = "CREATE TABLE " + TABLE_MESSAGE + "("
                + MESSAGE_ID + " INTEGER PRIMARY KEY," + MESSAGE_SENDER_ID + " TEXT," + MESSAGE_RECEIVER_ID + " TEXT," + MESSAGE_CONTENT + " TEXT" + ")";

        String CREATE_DEVICE_TABLE = "CREATE TABLE " + TABLE_DEVICE + "("
                + DEVICE_ID + " INTEGER PRIMARY KEY," + DEVICE_XBEE_DEVICE_NUMBER + " TEXT" + ")";

        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_MESSAGE_TABLE);
        db.execSQL(CREATE_DEVICE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEVICE);

        // Create tables again
        onCreate(db);
    }

    // delete DB
    void deleteDB()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USER, null, null);
        db.delete(TABLE_MESSAGE, null, null);
        db.delete(TABLE_DEVICE, null, null);
        //String myPath = DATABASE_PATH + DATABASE_NAME;
        //SQLiteDatabase.deleteDatabase(new File(myPath));
    }
    // code to add the new user
    void addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(USER_XBEE_DEVICE_NUMBER, user.getXbeeDeviceNumber());
        values.put(USER_NAME, user.getName());// User Chats

        // Inserting Row
        db.insert(TABLE_USER, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    // code to get the single user
    User getUser(Integer id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USER, new String[]{USER_ID, USER_XBEE_DEVICE_NUMBER, USER_NAME}, USER_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        User user = new User(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1),
                cursor.getString(2));
        // return user
        return user;
    }

    // code to get all users in a list view
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<User>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_USER;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setId(Integer.parseInt(cursor.getString(0)));
                user.setXbeeDeviceNumber(cursor.getString(1));
                user.setName(cursor.getString(2));
                // Adding contact to list
                userList.add(user);
            } while (cursor.moveToNext());
        }

        // return contact list
        return userList;
    }

    // code to update the single user
    public int updateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(USER_XBEE_DEVICE_NUMBER, user.getXbeeDeviceNumber());

        // updating row
        return db.update(TABLE_USER, values, USER_ID + " = ?",
                new String[]{String.valueOf(user.getId())});
    }

    // Deleting single user
    public void deleteUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USER, USER_ID + " = ?",
                new String[]{String.valueOf(user.getId())});
        db.close();
    }

    // Getting users Count
    public int getUsersCount() {
        String countQuery = "SELECT  * FROM " + TABLE_USER;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

    public void insertMessage(Message message){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MESSAGE_SENDER_ID,message.getSenderId());
        values.put(MESSAGE_RECEIVER_ID,message.getReceiverId());
        values.put(MESSAGE_CONTENT,message.getContent());

        db.insert(TABLE_MESSAGE,null,values);
        db.close();
    }

    // code to get all users in a list view
    public List<Message> getAllMessages() {
        List<Message> messagesList = new ArrayList<Message>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_MESSAGE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(Integer.parseInt(cursor.getString(0)));
                message.setSenderId(cursor.getString(1));
                message.setReceiverId(cursor.getString(2));
                message.setContent(cursor.getString(3));
                // Adding contact to list
                messagesList.add(message);
            } while (cursor.moveToNext());
        }

        // return contact list
        return messagesList;
    }

    // code to add the new device
    void addDevice(Device device) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DEVICE_XBEE_DEVICE_NUMBER, device.getXbeeDeviceNumber());

        // Inserting Row
        db.insert(TABLE_DEVICE, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    // code to get all devices in a list view
    public List<Device> getAllDevices() {
        List<Device> deviceList = new ArrayList<Device>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_DEVICE;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Device device = new Device();
                device.setId(Integer.parseInt(cursor.getString(0)));
                device.setXbeeDeviceNumber(cursor.getString(1));
                // Adding device to list
                deviceList.add(device);
            } while (cursor.moveToNext());
        }

        // return contact list
        return deviceList;
    }
}