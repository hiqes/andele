# Android Delegate (Andele) for Permission Handling

The name Andele was selected to be both a combination of Android and Delegate
and the word "andele" is slang in English for "hurry up", derived from the
Spanish ''ándale''.  It fit perfectly with the purpose of the library: provide
Android delegate support for permissions and the ability for developers to
quickly deploy the new permissions model in Marshmallow. The API can also be
used to quickly retrofit legacy apps to support the new permissions model.
In both cases, the library will work seamlessly with versions prior to API 23,
down to API 15.

With the new Android permissions model introduced in Android 6.0 (API 23),
developers now must be concerned with having the appropriate permissions for
using protected APIs.  While this is not incredibly difficult to do, it does
require a fair amount of boilerplate code and handling as well as some
interesting interactions.  The purpose of this library is to provide a simple,
extensible mechanism to handle this for app developers.  The main goals of the
library are:

* Provide a simple to use API
* Wrap protected API calls inside of an extensible class which handles checking,
requesting and notifying the user of permissions needs
* Support Activity and Fragment based UIs
* Present UI components which follow Material Design
* Allow app devs to customize the UX presented by the andele library

The library's support of UX controls is based upon Google's recommendations for
UX handling with runtime permissions.  Permissions are placed into one of these
quadrants by its PermissionUse and can be CRITICAL, ESSENTIAL, FEATURE and
OPTIONAL:

```
                              Critical
                                 |
                                 |
                  Educate up     |    Ask up front
                    front        |     (CRITICAL)
                  (ESSENTIAL)    |
                                 |
                                 |
non-obvious  ------------------------------------------ Obvious
                                 |
                                 |
                  Educate in     |    Ask in context
                   context       |      (FEATURE)
                  (OPTIONAL)     |
                                 |
                              Secondary
```

Andele supports permission requests via Activity, AppCompatActivity (or its
derivatives), Fragment and support Fragment.  Instead of providing a simple
wrapper around the new Activity/Fragment APIs, Andele uses the notion of passing
it an action to perform (via callback) which is protected by a permission.  It
then handles the permission check and if the app has the permission it
calls the action callback in the same context in which it was requested.
For example:

```
    @Override
    public void onClick(View view) {
        ProtectedAction.Builder builder = new ProtectedAction.Builder();
        builder.withPermission(Manifest.permission.READ_CALL_LOG)
               .withUsage(PermissionUse.FEATURE)
               .listener(this)
               .actionCallback(new ProtectedAction.ActionCallback() {
                   @Override
                   public void doAction(ProtectedAction action) {
                       Log.d(TAG, "Last call: " + CallLog.Calls.getLastOutgoingCall(this));
                   }
               })
               .userPromptCallback(this);
        Andele.checkAndExecute(this, builder.build());
```

Since Andele does not require the app to subclass a special Activity or
Fragment, the app must also call through to Andele from its
`onRequestPermissionsResult()` method.

```
     @Override
     public static void onRequestPermissionsResult(int reqCode, String[] permissions, int[] grantResults) {
         Andele.onRequestPermissionsResult(reqCode, permissions, grantResults));
     }
```


