CREATE TABLE perfiles (
    id_perfil BIGSERIAL PRIMARY KEY,
    id_provider UUID UNIQUE,
    email VARCHAR(255),
    nombre VARCHAR(200) NOT NULL,
    apellido VARCHAR(200),
    direccion_envio VARCHAR(200),
    telefono VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);