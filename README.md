Schoology To Eclipse
====================

This is a little tool to convert a zip of assignment submissions exported from
Schoology into something that can be imported into Eclipse in one go.

Usage
-----

1. Import this project into Eclipse.
2. In Schoology, navigate to the assingment and click the Download All button (green arrow at the top of the list of submissions).
3. Run the project. In the dialog that appears, select the zip you downloaded from Schoology.
4. In Eclipse, choose File > Import, then choose "Existing Projects into Workspace" and click next.
5. Choose "Select archive file" and click "Browse…", then choose the zip file generated by this tool (it should be in your downloads folder, with a name starting with ECLIPSE_).
6. Make sure all the projects are checked.
7. In the "Working sets" section at the bottom, click "New…", choose Java, click Next, enter the name of the project, and click Finish.
  - This allows us to keep the student projects seperate in our project list and keep things vaguely organized.
8. Click Finish in the import dialog.
9. If you're doing this for the first time, in Eclipse, click the arrow icon at the top of the Package Explorer, and choose Top Level Elements > Working Sets.

At this point, your Package Explorer sohuld be showing a "folder" (working set) named after the assignment, containing a project for each student (or multiple projects if they've resubmitted).
