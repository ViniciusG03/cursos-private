CREATE TABLE modules (
    id uuid PRIMARY KEY default gen_random_uuid(),
    course_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    position INT NOT NULL CHECK (position > 0),

    CONSTRAINT fk_course FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT uq_courseid_position UNIQUE (course_id, position)
);