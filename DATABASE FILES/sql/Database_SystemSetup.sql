CREATE TABLE schema_version (version_number INT PRIMARY KEY);

CREATE TABLE plots (
    plot_number VARCHAR(20) PRIMARY KEY,
    section VARCHAR(60) NOT NULL,
    CONSTRAINT plot_nonempty CHECK (LENGTH(TRIM(plot_number)) > 0),
    CONSTRAINT section_nonempty CHECK (LENGTH(TRIM(section)) > 0)
);

CREATE TABLE burials (
    record_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    deceased_name VARCHAR(120) NOT NULL,
    birth_date DATE NOT NULL,
    death_date DATE NOT NULL,
    burial_date DATE NOT NULL,
    plot_number VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT burial_plot FOREIGN KEY (plot_number) REFERENCES plots(plot_number),
    CONSTRAINT name_nonempty CHECK (LENGTH(TRIM(deceased_name)) > 0),
    CONSTRAINT birth_before_death CHECK (birth_date <= death_date),
    CONSTRAINT death_before_burial CHECK (death_date <= burial_date)
);

INSERT INTO schema_version VALUES (1);



SELECT deceased_name AS name, birth_date, death_date
FROM burials ORDER BY deceased_name;

-- Full burial information including identity and plot
SELECT record_id, deceased_name, birth_date, death_date, burial_date, plot_number
FROM burials ORDER BY record_id;

-- Plot status is derived, not manually stored
SELECT p.plot_number, p.section,
       CASE WHEN b.record_id IS NULL THEN 'AVAILABLE' ELSE 'OCCUPIED' END AS status
FROM plots p LEFT JOIN burials b ON p.plot_number = b.plot_number
ORDER BY p.plot_number;

-- After optional demo seeding: 100 total plots and 15 burials
SELECT COUNT(*) AS total_plots FROM plots;
SELECT COUNT(*) AS total_burials FROM burials;

-- Available plots after optional demo seeding: 85
SELECT COUNT(*) AS available_plots FROM plots p
WHERE NOT EXISTS (SELECT 1 FROM burials b WHERE b.plot_number = p.plot_number);
