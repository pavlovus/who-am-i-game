CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    games_played INT DEFAULT 0,
    games_won INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE characters (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'approved', -- 'approved', 'pending'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE games (
    id SERIAL PRIMARY KEY,
    room_code VARCHAR(10) UNIQUE NOT NULL,
    player1_id INT REFERENCES users(id),
    player2_id INT REFERENCES users(id),
    winner_id INT REFERENCES users(id),
    character_id INT REFERENCES characters(id),
    total_questions INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'in_progress', -- 'in_progress', 'finished', 'aborted'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert some default characters
INSERT INTO characters (name, category) VALUES 
('Harry Potter', 'Movies & TV Series'),
('Geralt of Rivia', 'Video Games'),
('Albert Einstein', 'Real People'),
('Lion', 'Animals');
