CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    position INT NOT NULL,

    CONSTRAINT fk_lessons_module FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT ck_lessons_position_positive CHECK (position > 0),
    CONSTRAINT uq_lessons_module_position UNIQUE (module_id, position)
);
