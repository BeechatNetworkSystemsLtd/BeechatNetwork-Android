package com.beechat.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHandler extends SQLiteOpenHelper {
    //private static final String DATABASE_PATH = "//data/data/com.beechat.network/databases/";
    private static final String DATABASE_NAME = "XBEE.DB";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String USERS_ID = "id";
    private static final String USERS_USERNAME = "username";
    private static final String USERS_PASSWORD = "password";

    private static final String TABLE_CONTACTS = "contacts";
    private static final String CONTACTS_ID = "id";
    private static final String CONTACTS_USER_ID = "user_id";
    private static final String CONTACTS_XBEE_DEVICE_NUMBER = "xbee_device_number";
    private static final String CONTACTS_NAME = "name";

    private static final String TABLE_MESSAGES = "messages";
    private static final String MESSAGES_ID = "id";
    private static final String MESSAGES_SENDER_ID = "sender_id";
    private static final String MESSAGES_XBEE_DEVICE_NUMBER_SENDER = "xbee_device_number_sender";
    private static final String MESSAGES_RECEIVER_ID = "receiver_id";
    private static final String MESSAGES_XBEE_DEVICE_NUMBER_RECEIVER = "xbee_device_number_receiver";
    private static final String MESSAGES_CONTENT = "content";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //3rd argument to be passed is CursorFactory instance
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + USERS_ID + " INTEGER PRIMARY KEY," + USERS_USERNAME + " TEXT," + USERS_PASSWORD + " TEXT" + ")";

        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "("
                + CONTACTS_ID + " INTEGER PRIMARY KEY," + CONTACTS_USER_ID + " TEXT," + CONTACTS_XBEE_DEVICE_NUMBER + " TEXT," + CONTACTS_NAME + " TEXT" + ")";

        String CREATE_MESSAGES_TABLE = "CREATE TABLE " + TABLE_MESSAGES + "("
                + MESSAGES_ID + " INTEGER PRIMARY KEY," + MESSAGES_SENDER_ID + " TEXT," + MESSAGES_XBEE_DEVICE_NUMBER_SENDER + " TEXT," + MESSAGES_RECEIVER_ID + " TEXT," + MESSAGES_XBEE_DEVICE_NUMBER_RECEIVER + " TEXT," + MESSAGES_CONTENT + " TEXT" + ")";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_CONTACTS_TABLE);
        db.execSQL(CREATE_MESSAGES_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        // Create tables again
        onCreate(db);
    }

    // Deleting database
    void deleteDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USERS, null, null);
        db.delete(TABLE_CONTACTS, null, null);
        db.delete(TABLE_MESSAGES, null, null);
    }

    // Add user to table Users
    public Boolean addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(USERS_USERNAME, username);
        contentValues.put(USERS_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, contentValues);
        if (result == -1) return false;
        else
            return true;
    }

    // check username in table Users
    public Boolean checkUsername(String username) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from " + TABLE_USERS + " where username = ?", new String[]{username});
        if (cursor.getCount() > 0)
            return true;
        else
            return false;
    }

    // check username and password in table Users
    public Boolean checkUsernamePassword(String username, String password) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from " + TABLE_USERS + " where username = ? and password = ?", new String[]{username, password});
        if (cursor.getCount() > 0)
            return true;
        else
            return false;
    }

    // Get list of usernames from table Users
    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_USERS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String name = new String();
                name = cursor.getString(1);
                names.add(name);
            } while (cursor.moveToNext());
        }
        return names;
    }

    // Adding new contact to table Contacts
    void addContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CONTACTS_USER_ID, contact.getUserId());
        values.put(CONTACTS_XBEE_DEVICE_NUMBER, contact.getXbeeDeviceNumber());
        values.put(CONTACTS_NAME, contact.getName());// User Chats

        // Inserting Row
        db.insert(TABLE_CONTACTS, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    // Code to get the single contact
    Contact getContact(Integer id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CONTACTS, new String[]{CONTACTS_ID, CONTACTS_USER_ID, CONTACTS_XBEE_DEVICE_NUMBER, CONTACTS_NAME}, CONTACTS_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        Contact user = new Contact(Integer.parseInt(cursor.getString(0)),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3));
        // return user
        return user;
    }

    // Check count of users in table Users
    public boolean checkCountUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        String count = "SELECT count(*) FROM " + TABLE_USERS;
        Cursor mcursor = db.rawQuery(count, null);
        mcursor.moveToFirst();
        int icount = mcursor.getInt(0);
        if (icount > 0) return true;
        else return false;
    }

    // Code to get all contacts in a list view
    public List<Contact> getAllContacts() {
        List<Contact> contactList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_CONTACTS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.setId(Integer.parseInt(cursor.getString(0)));
                contact.setUserId(cursor.getString(1));
                contact.setXbeeDeviceNumber(cursor.getString(2));
                contact.setName(cursor.getString(3));
                contactList.add(contact);
            } while (cursor.moveToNext());
        }
        return contactList;
    }

    // Code to update the single contact
    public int updateContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CONTACTS_USER_ID, contact.getXbeeDeviceNumber());
        values.put(CONTACTS_XBEE_DEVICE_NUMBER, contact.getXbeeDeviceNumber());
        values.put(CONTACTS_NAME, contact.getName());

        return db.update(TABLE_CONTACTS, values, CONTACTS_XBEE_DEVICE_NUMBER + " = ?",
                new String[]{String.valueOf(contact.xbee_device_number)});
    }

    // Deleting single contact
    public void deleteContact(Contact contact) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CONTACTS, CONTACTS_ID + " = ?",
                new String[]{String.valueOf(contact.getId())});
        db.close();
    }

    // Getting count of contacts
    public int getContactsCount() {
        String countQuery = "SELECT  * FROM " + TABLE_CONTACTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        return cursor.getCount();
    }

    // Insert message to table Messages
    public void insertMessage(Message message) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MESSAGES_SENDER_ID, message.getSenderId());
        values.put(MESSAGES_XBEE_DEVICE_NUMBER_SENDER, message.getXbeeSender());
        values.put(MESSAGES_RECEIVER_ID, message.getReceiverId());
        values.put(MESSAGES_XBEE_DEVICE_NUMBER_RECEIVER, message.getXbeeReceiver());
        values.put(MESSAGES_CONTENT, message.getContent());

        db.insert(TABLE_MESSAGES, null, values);
        db.close();
    }

    // Code to get all messages in a list view
    public List<Message> getAllMessages(String chatSenderId, String chatXbeeSender, String chatReceiverId, String chatXbeeReceiver) {
        List<Message> messagesList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_MESSAGES + " WHERE " + MESSAGES_SENDER_ID + " = " + chatSenderId + " and " +
                MESSAGES_XBEE_DEVICE_NUMBER_SENDER + " = " + chatXbeeSender + " and " + MESSAGES_RECEIVER_ID + " = " + chatReceiverId + " and " +
                MESSAGES_XBEE_DEVICE_NUMBER_RECEIVER + " = " + chatXbeeReceiver;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Message message = new Message();
                message.setId(Integer.parseInt(cursor.getString(0)));
                message.setSenderId(cursor.getString(1));
                message.setXbeeSender(cursor.getString(2));
                message.setReceiverId(cursor.getString(3));
                message.setXbeeReceiver(cursor.getString(4));
                message.setContent(cursor.getString(5));
                messagesList.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messagesList;
    }
}