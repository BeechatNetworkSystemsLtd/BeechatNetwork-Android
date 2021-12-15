package com.beechat.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/***
 *  --- DatabaseHandler ---
 *  The class that is responsible for the data processing functions in the database.
 ***/
public class DatabaseHandler extends SQLiteOpenHelper {
    // Variables
    private final Context mContext;

    // Constants
    private static final String SECRET_KEY = "secret";
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
        mContext = context;
        //3rd argument to be passed is CursorFactory instance
    }

    /***
     *  --- onCreate(SQLiteDatabase) ---
     *  The function of the creating tables database.
     *
     * @param db The current database.
     ***/
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

    /***
     *  --- onUpgrade(SQLiteDatabase, int, int) ---
     *  The function of the upgrading database.
     *
     * @param db The current database.
     * @param oldVersion The number old version DB.
     * @param newVersion The number new version DB..
     ***/
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        // Create tables again
        onCreate(db);
    }

    /***
     *  --- deleteDB() ---
     *  The function of the deleting database.
     ***/
    void deleteDB() {
        SQLiteDatabase.loadLibs(mContext);
        SQLiteDatabase db = this.getWritableDatabase(SECRET_KEY);
        db.delete(TABLE_USERS, null, null);
        db.delete(TABLE_CONTACTS, null, null);
        db.delete(TABLE_MESSAGES, null, null);
    }

    /***
     *  --- addUser(String, String) ---
     *  The function of adding a new user in 'Users' table.
     *
     *  @param username The generated username.
     *  @param password The password.
     ***/
    public Boolean addUser(String username, String password) {
        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getWritableDatabase(SECRET_KEY);

        ContentValues contentValues = new ContentValues();
        contentValues.put(USERS_USERNAME, username);
        contentValues.put(USERS_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, contentValues);
        return result != -1;
    }

    /***
     *  --- checkUsername(String) ---
     *  The function of checking username in 'Users' table.
     *
     *  @param username The generated username.
     ***/
    public Boolean checkUsername(String username) {
        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getReadableDatabase(SECRET_KEY);
        Cursor cursor = db.rawQuery("Select * from " + TABLE_USERS + " where username = ?", new String[]{username});
        return cursor.getCount() > 0;
    }

    /***
     *  --- checkUsernamePassword(String, String) ---
     *  The function of checking username and password in 'Users' table.
     *
     *  @param username The generated username.
     *  @param password The password.
     ***/
    public Boolean checkUsernamePassword(String username, String password) {
        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getReadableDatabase(SECRET_KEY);
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE USERNAME = ? AND PASSWORD = ?", new String[]{username, password});
        return cursor.getCount() > 0;
    }

    /***
     *  --- getNames() ---
     *  The function of getting all usernames from the 'Users' table.
     ***/
    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_USERS;

        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getReadableDatabase(SECRET_KEY);
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(1);
                names.add(name);
            } while (cursor.moveToNext());
        }
        return names;
    }

    /***
     *  --- addContact(Contact) ---
     *  The function of adding new contact to the 'Contacts' table.
     *
     * @param contact The new contact.
     ***/
    void addContact(Contact contact) {
        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getWritableDatabase(SECRET_KEY);

        ContentValues values = new ContentValues();
        values.put(CONTACTS_USER_ID, contact.getUserId());
        values.put(CONTACTS_XBEE_DEVICE_NUMBER, contact.getXbeeDeviceNumber());
        values.put(CONTACTS_NAME, contact.getName());// User Chats

        // Inserting Row
        db.insert(TABLE_CONTACTS, null, values);
        //2nd argument is String containing nullColumnHack
        db.close(); // Closing database connection
    }

    /***
     *  --- checkCountUsers() ---
     *  The function of checking count of users in 'Users' table.
     ***/
    public boolean checkCountUsers() {
        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getWritableDatabase(SECRET_KEY);
        String queryCount = "SELECT COUNT(*) FROM " + TABLE_USERS;
        Cursor cursor = db.rawQuery(queryCount, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        return count > 0;
    }

    /***
     *  --- getAllContacts() ---
     *  The function of getting all contacts from the 'Contacts' table.
     ***/
    public List<Contact> getAllContacts() {
        List<Contact> contactList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_CONTACTS;

        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getReadableDatabase(SECRET_KEY);

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

    /***
     *  --- updateContact(Contact) ---
     *  The function of updating current contact in 'Contacts' table.
     *
     *  @param contact The current contact.
     ***/
    public int updateContact(Contact contact) {
        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getWritableDatabase(SECRET_KEY);

        ContentValues values = new ContentValues();
        values.put(CONTACTS_USER_ID, contact.getXbeeDeviceNumber());
        values.put(CONTACTS_XBEE_DEVICE_NUMBER, contact.getXbeeDeviceNumber());
        values.put(CONTACTS_NAME, contact.getName());

        return db.update(TABLE_CONTACTS, values, CONTACTS_USER_ID + " = ?",
                new String[]{String.valueOf(contact.user_id)});
    }

    /***
     *  --- insertMessage(Message) ---
     *  The function to insert message to the 'Messages' table.
     *
     *  @param message Transmitted message.
     ***/
    public void insertMessage(Message message) {
        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getWritableDatabase(SECRET_KEY);

        ContentValues values = new ContentValues();
        values.put(MESSAGES_SENDER_ID, message.getSenderId());
        values.put(MESSAGES_XBEE_DEVICE_NUMBER_SENDER, message.getXbeeSender());
        values.put(MESSAGES_RECEIVER_ID, message.getReceiverId());
        values.put(MESSAGES_XBEE_DEVICE_NUMBER_RECEIVER, message.getXbeeReceiver());
        values.put(MESSAGES_CONTENT, message.getContent());

        db.insert(TABLE_MESSAGES, null, values);
        db.close();
    }

    /***
     *  --- getAllMessages(String, String, String, String) ---
     *  The function for retrieving all messages from 'Messages' table.
     *
     *  @param chatSenderId My user id.
     *  @param chatXbeeSender My Xbee address.
     *  @param chatReceiverId Selected user id.
     *  @param chatXbeeReceiver Selected Xbee address.
     ***/
    public List<Message> getAllMessages(String chatSenderId, String chatXbeeSender, String chatReceiverId, String chatXbeeReceiver) {
        List<Message> messagesList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_MESSAGES + " WHERE " + MESSAGES_SENDER_ID + " = " + chatSenderId + " and " +
                MESSAGES_XBEE_DEVICE_NUMBER_SENDER + " = " + chatXbeeSender + " and " + MESSAGES_RECEIVER_ID + " = " + chatReceiverId + " and " +
                MESSAGES_XBEE_DEVICE_NUMBER_RECEIVER + " = " + chatXbeeReceiver;
        SQLiteDatabase.loadLibs(mContext.getApplicationContext());
        SQLiteDatabase db = this.getReadableDatabase(SECRET_KEY);
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
        return messagesList;
    }
}