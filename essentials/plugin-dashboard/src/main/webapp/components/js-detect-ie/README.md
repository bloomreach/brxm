detect-ie
=========

Helps to detect Internet Explorer, its current version, and its native version.

Too often do people use the `userAgent` of the browser to detect (which can be [spoofed quite easily](http://www.howtogeek.com/113439/how-to-change-your-browsers-user-agent-without-installing-any-extensions/)), or conditional comments. Neither are reliable.

Conditional Comments are [no longer supported in IE 10](http://msdn.microsoft.com/en-us/library/ie/hh801214\(v%3Dvs.85\).aspx), making them an invalid way to detect IE and its versions. ( another [reference](http://www.sitepoint.com/microsoft-drop-ie10-conditional-comments/) )

[Conditional Compilation](http://msdn.microsoft.com/en-us/library/7kx09ct1\(v%3Dvs.80\).aspx), a different feature, also only supported in IE, allows you to find the version of the IE browser more correctly. ( another [reference](http://www.javascriptkit.com/javatutors/conditionalcompile.shtml) )

Use
---

A variable (`Object`), called `IE`, is created.

This object has 3 properties:

 - `isTheBrowser`, a **boolean**, telling whether the current browser is IE or not.
 - `actualVersion`, a **string**, telling the true version of the browser. For example, if IE 10 is being used, but the browser/document mode (changed in Developer Tools) is for IE 9, this will still return "10".
 - `currentVersion`, a **string**, telling the current acting version of the browser. In the previous example, this will return "9".
