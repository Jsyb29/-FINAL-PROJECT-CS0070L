# -FINAL-PROJECT-CS0070L

## OVERVIEW
The Simple Graveyard System is an electronic ledger to track records of burials. The system records two types of information: 
Plots: a numbered burial location (i.e. A-001), grouped in sections (i.e. Section A).
Burials: the name of the deceased, date of birth, date of death, burial date, and the plot where the person is buried. A plot’s status (Available or Occupied) is not entered by a user. It is determined by whether a burial record exists for that plot. This ensures the data does not get out of sync. The system uses a layered design (i.e. UI → Service → Repository → Database), which means the business rules are in one location, and the GUI never communicates to a database. The database is embedded, which means there is no server to configure.

## FEAUTURES


###### Plot Management:
- Add plots
- Edit a plot’s section
- Delete a plot only if it is empty (i.e. burial records are protected)
- Filter plots by section, available or occupied status

###### Burial Management:
- Record a burial only if the plot is available
- Edit a burial’s name, date of birth, and date of death
- View all burial records
- Search and Sort
- Case-insensitive name search and plot number search
- Sort results by name, date of death, plot number
- Edit record from search results

###### Dashboard:
- A real-time report of the total number of plots, available and occupied plots and occupancy percent

###### Data integrity:
- Strict YYYY-MM-DD parsing (impossible dates like 2025-02-29 are rejected)
- Birth ≤ death ≤ burial date, enforced in both the app and the database
- Date of death cannot be in the future
- One burial per plot (UNIQUE constraint)
- Input normalization (plot numbers upper-cased and de-spaced; names trimmed)

 ## INSTALLATION PROCESS
 
##### Prerequisites

- Net Bens
- JDK
- Net Beans
- Apache Derby ```
