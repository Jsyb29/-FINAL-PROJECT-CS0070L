# -FINAL-PROJECT-CS0070L

## OVERVIEW
The Simple Graveyard System is an electronic ledger to track records of burials. The system records two types of information: 
Plots: a numbered burial location (i.e. A-001), grouped in sections (i.e. Section A).
Burials: the name of the deceased, date of birth, date of death, burial date, and the plot where the person is buried. A plot’s status (Available or Occupied) is not entered by a user. It is determined by whether a burial record exists for that plot. This ensures the data does not get out of sync. The system uses a layered design (i.e. UI → Service → Repository → Database), which means the business rules are in one location, and the GUI never communicates to a database. The database is embedded, which means there is no server to configure.
