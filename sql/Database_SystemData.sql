-- FICTIONAL DECEASED RECORDS - SIMPLE GRAVEYARD SYSTEM

-- Existing plots are preserved. Existing burials are not updated/deleted.
-- Burials are inserted only into plots that have no burial record.
-- On an EMPTY initialized database: 100 plots, 15 occupied, 85 available.
-- The database generates RECORD_ID automatically, so it is omitted below.
-- This script does NOT create/drop tables or change the database schema.

-- Validation: exported from the project's Java seeder; 200 unique plot keys,
-- checked. Actual Derby execution was unavailable in the build environment.

-- Plots
INSERT INTO APP.PLOTS (plot_number, section)
SELECT v.plot_number, TRIM(v.section)
FROM (VALUES
    ('A-001', 'Section A'),
    ('A-002', 'Section A'),
    ('A-003', 'Section A'),
    ('A-004', 'Section A'),
    ('A-005', 'Section A'),
    ('A-006', 'Section A'),
    ('A-007', 'Section A'),
    ('A-008', 'Section A'),
    ('A-009', 'Section A'),
    ('A-010', 'Section A'),
    ('A-011', 'Section A'),
    ('A-012', 'Section A'),
    ('A-013', 'Section A'),
    ('A-014', 'Section A'),
    ('A-015', 'Section A'),
    ('A-016', 'Section A'),
    ('A-017', 'Section A'),
    ('A-018', 'Section A'),
    ('A-019', 'Section A'),
    ('A-020', 'Section A'),
    ('A-021', 'Section A'),
    ('A-022', 'Section A'),
    ('A-023', 'Section A'),
    ('A-024', 'Section A'),
    ('A-025', 'Section A'),
   
    ('B-001', 'Section B'),
    ('B-002', 'Section B'),
    ('B-003', 'Section B'),
    ('B-004', 'Section B'),
    ('B-005', 'Section B'),
    ('B-006', 'Section B'),
    ('B-007', 'Section B'),
    ('B-008', 'Section B'),
    ('B-009', 'Section B'),
    ('B-010', 'Section B'),
    ('B-011', 'Section B'),
    ('B-012', 'Section B'),
    ('B-013', 'Section B'),
    ('B-014', 'Section B'),
    ('B-015', 'Section B'),
    ('B-016', 'Section B'),
    ('B-017', 'Section B'),
    ('B-018', 'Section B'),
    ('B-019', 'Section B'),
    ('B-020', 'Section B'),
    ('B-021', 'Section B'),
    ('B-022', 'Section B'),
    ('B-023', 'Section B'),
    ('B-024', 'Section B'),
    ('B-025', 'Section B'),
   
    ('C-001', 'Section C'),
    ('C-002', 'Section C'),
    ('C-003', 'Section C'),
    ('C-004', 'Section C'),
    ('C-005', 'Section C'),
    ('C-006', 'Section C'),
    ('C-007', 'Section C'),
    ('C-008', 'Section C'),
    ('C-009', 'Section C'),
    ('C-010', 'Section C'),
    ('C-011', 'Section C'),
    ('C-012', 'Section C'),
    ('C-013', 'Section C'),
    ('C-014', 'Section C'),
    ('C-015', 'Section C'),
    ('C-016', 'Section C'),
    ('C-017', 'Section C'),
    ('C-018', 'Section C'),
    ('C-019', 'Section C'),
    ('C-020', 'Section C'),
    ('C-021', 'Section C'),
    ('C-022', 'Section C'),
    ('C-023', 'Section C'),
    ('C-024', 'Section C'),
    ('C-025', 'Section C'),
  
    ('D-001', 'Section D'),
    ('D-002', 'Section D'),
    ('D-003', 'Section D'),
    ('D-004', 'Section D'),
    ('D-005', 'Section D'),
    ('D-006', 'Section D'),
    ('D-007', 'Section D'),
    ('D-008', 'Section D'),
    ('D-009', 'Section D'),
    ('D-010', 'Section D'),
    ('D-011', 'Section D'),
    ('D-012', 'Section D'),
    ('D-013', 'Section D'),
    ('D-014', 'Section D'),
    ('D-015', 'Section D'),
    ('D-016', 'Section D'),
    ('D-017', 'Section D'),
    ('D-018', 'Section D'),
    ('D-019', 'Section D'),
    ('D-020', 'Section D'),
    ('D-021', 'Section D'),
    ('D-022', 'Section D'),
    ('D-023', 'Section D'),
    ('D-024', 'Section D'),
    ('D-025', 'Section D')
  
) AS v (plot_number, section)
WHERE NOT EXISTS (
    SELECT 1 FROM APP.PLOTS existing_plot
    WHERE existing_plot.plot_number = v.plot_number
);

-- Fictional Deceased Database
-- DEATH_DATE is the day of death; BURIAL_DATE is a separate, later date.
INSERT INTO APP.BURIALS
    (deceased_name, birth_date, death_date, burial_date, plot_number)
SELECT TRIM(v.deceased_name), v.birth_date, v.death_date, v.burial_date,
       v.plot_number
FROM (VALUES
    ('Arturo Santos', DATE('1942-10-07'), DATE('2011-09-01'), DATE('2011-09-04'), 'B-046'),
    ('Beatriz Santos', DATE('1939-06-27'), DATE('2018-12-21'), DATE('2018-12-24'), 'C-028'),
    ('Carlos Santos', DATE('1967-11-06'), DATE('2018-08-09'), DATE('2018-08-12'), 'D-032'),
    ('Diana Santos', DATE('1969-12-05'), DATE('2023-10-20'), DATE('2023-10-23'), 'D-040'),
    ('Eduardo Santos', DATE('1954-05-19'), DATE('2023-12-14'), DATE('2023-12-17'), 'C-034'),
    ('Felisa Santos', DATE('1933-08-08'), DATE('2018-06-05'), DATE('2018-06-08'), 'A-008'),
    ('Gabriel Santos', DATE('1956-01-01'), DATE('2014-12-21'), DATE('2014-12-24'), 'C-049'),
    ('Helena Santos', DATE('1955-05-21'), DATE('2015-11-23'), DATE('2015-11-26'), 'C-042'),
    ('Isabel Santos', DATE('1960-09-19'), DATE('2021-06-15'), DATE('2021-06-18'), 'C-030'),
    ('Joaquin Santos', DATE('1950-09-28'), DATE('2018-06-01'), DATE('2018-06-04'), 'D-017'),
    ('Lourdes Santos', DATE('1930-10-12'), DATE('2013-12-18'), DATE('2013-12-21'), 'B-018'),
    ('Manuel Santos', DATE('1966-11-01'), DATE('2011-09-09'), DATE('2011-09-12'), 'C-033'),
    ('Nora Santos', DATE('1931-01-23'), DATE('2018-01-28'), DATE('2018-01-31'), 'A-033'),
    ('Oscar Santos', DATE('1932-02-25'), DATE('2012-02-02'), DATE('2012-02-05'), 'D-023'),
    ('Pilar Santos', DATE('1966-02-11'), DATE('2017-10-06'), DATE('2017-10-09'), 'A-020'),
   
) AS v (deceased_name, birth_date, death_date, burial_date, plot_number)
WHERE EXISTS (
    SELECT 1 FROM APP.PLOTS existing_plot
    WHERE existing_plot.plot_number = v.plot_number
)
AND NOT EXISTS (
    SELECT 1 FROM APP.BURIALS existing_burial
    WHERE existing_burial.plot_number = v.plot_number
);

-- Display
SELECT deceased_name AS "Name",
       birth_date AS "Birth date",
       death_date AS "Day of death"
FROM APP.BURIALS
ORDER BY deceased_name;

-- Full record information (ID is generated automatically).
SELECT record_id, deceased_name, birth_date, death_date, burial_date, plot_number
FROM APP.BURIALS
ORDER BY record_id;

-- Expected on an initially empty database: 100 / 15 / 85.
SELECT COUNT(*) AS total_plots FROM APP.PLOTS;
SELECT COUNT(*) AS occupied_plots FROM APP.BURIALS;
SELECT COUNT(*) AS available_plots
FROM APP.PLOTS p
WHERE NOT EXISTS (
    SELECT 1 FROM APP.BURIALS b WHERE b.plot_number = p.plot_number
);
