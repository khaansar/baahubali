ALTER TABLE categories
    ADD COLUMN slug VARCHAR(255) NULL;

ALTER TABLE test_series
    ADD COLUMN slug VARCHAR(255) NULL;

ALTER TABLE mock_tests
    ADD COLUMN slug VARCHAR(255) NULL;

UPDATE categories
SET slug = NULLIF(
    REGEXP_REPLACE(
        LOWER(TRIM(name)),
        '[^a-z0-9]+',
        '-'
    ),
    ''
)
WHERE slug IS NULL;

UPDATE test_series
SET slug = NULLIF(
    REGEXP_REPLACE(
        LOWER(TRIM(title)),
        '[^a-z0-9]+',
        '-'
    ),
    ''
)
WHERE slug IS NULL;

UPDATE mock_tests
SET slug = NULLIF(
    REGEXP_REPLACE(
        LOWER(TRIM(title)),
        '[^a-z0-9]+',
        '-'
    ),
    ''
)
WHERE slug IS NULL;

UPDATE categories
SET slug = CONCAT(
    COALESCE(slug, 'category'),
    '-',
    LEFT(REPLACE(id, '-', ''), 8)
)
WHERE slug IS NULL OR slug = '';

UPDATE test_series ts
JOIN (
    SELECT slug
    FROM test_series
    GROUP BY slug
    HAVING COUNT(*) > 1
) duplicates ON duplicates.slug = ts.slug
SET ts.slug = CONCAT(
    ts.slug,
    '-',
    LEFT(REPLACE(ts.id, '-', ''), 8)
);

UPDATE mock_tests mt
JOIN (
    SELECT slug
    FROM mock_tests
    GROUP BY slug
    HAVING COUNT(*) > 1
) duplicates ON duplicates.slug = mt.slug
SET mt.slug = CONCAT(
    mt.slug,
    '-',
    LEFT(REPLACE(mt.id, '-', ''), 8)
);

UPDATE test_series
SET slug = CONCAT(
    'test-series-',
    LEFT(REPLACE(id, '-', ''), 8)
)
WHERE slug IS NULL OR slug = '';

UPDATE mock_tests
SET slug = CONCAT(
    'mock-test-',
    LEFT(REPLACE(id, '-', ''), 8)
)
WHERE slug IS NULL OR slug = '';

ALTER TABLE categories
    MODIFY COLUMN slug VARCHAR(255) NOT NULL;

ALTER TABLE test_series
    MODIFY COLUMN slug VARCHAR(255) NOT NULL;

ALTER TABLE mock_tests
    MODIFY COLUMN slug VARCHAR(255) NOT NULL;

ALTER TABLE categories
    ADD CONSTRAINT uk_categories_slug UNIQUE (slug);

ALTER TABLE test_series
    ADD CONSTRAINT uk_test_series_slug UNIQUE (slug);

ALTER TABLE mock_tests
    ADD CONSTRAINT uk_mock_tests_slug UNIQUE (slug);