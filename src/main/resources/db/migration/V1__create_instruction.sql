CREATE TABLE instruction (
    id UUID PRIMARY KEY,
    instruction_id VARCHAR(22) NOT NULL UNIQUE,
    direction VARCHAR(3) NOT NULL,
    instruction_type VARCHAR(32) NOT NULL,
    source_format VARCHAR(32) NOT NULL,
    message_type VARCHAR(64),
    status VARCHAR(64) NOT NULL,
    workflow_code VARCHAR(64) NOT NULL,
    workflow_state VARCHAR(16) NOT NULL,
    current_business_function VARCHAR(64) NOT NULL,
    correlation_id UUID NOT NULL,
    source_instruction_id UUID,
    file_reference VARCHAR(1024),
    file_checksum VARCHAR(128),
    file_size_bytes BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_instruction_id_length
        CHECK (CHAR_LENGTH(instruction_id) = 22),
    CONSTRAINT ck_instruction_direction
        CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT ck_instruction_workflow_state
        CHECK (workflow_state IN ('PENDING', 'RUNNING', 'WAITING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_instruction_file_size
        CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0),
    CONSTRAINT fk_instruction_source
        FOREIGN KEY (source_instruction_id) REFERENCES instruction (id)
);

CREATE INDEX idx_instruction_correlation_id
    ON instruction (correlation_id);

CREATE INDEX idx_instruction_workflow_state
    ON instruction (workflow_state, current_business_function);

CREATE INDEX idx_instruction_source_instruction_id
    ON instruction (source_instruction_id);
