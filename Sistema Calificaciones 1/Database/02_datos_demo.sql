-- Datos ficticios para practicar el sistema. Ejecutar una sola vez,
-- conectado como CALIFICACIONES, después de 01_schema.sql.
-- Todos los usuarios demo usan como contraseña: password
-- La contraseña está guardada con BCrypt (hash de ejemplo de Spring Security).

DECLARE
  TYPE texto_10_t IS VARRAY(10) OF VARCHAR2(30);
  TYPE texto_4_t IS VARRAY(4) OF VARCHAR2(40);
  TYPE ids_t IS TABLE OF NUMBER INDEX BY PLS_INTEGER;

  v_nombres texto_10_t := texto_10_t(
    'Valentina','Santiago','Isabella','Sebastian','Camila',
    'Mateo','Mariana','Nicolas','Luciana','Samuel'
  );
  v_apellidos texto_10_t := texto_10_t(
    'Rodriguez','Gomez','Martinez','Lopez','Hernandez',
    'Garcia','Perez','Sanchez','Ramirez','Torres'
  );
  v_profesores texto_4_t := texto_4_t(
    'Profesor Demo 01','Profesor Demo 02','Profesor Demo 03','Profesor Demo 04'
  );
  v_materias texto_4_t := texto_4_t(
    'Calculo I','Programacion I','Fisica I','Bases de Datos'
  );
  v_hash CONSTANT VARCHAR2(60) :=
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';
  v_ids_profesores ids_t;
  v_ids_asignaturas ids_t;
  v_id_estudiante NUMBER;
  v_id_matricula NUMBER;
  v_correo VARCHAR2(160);
  v_indice_a PLS_INTEGER;
  v_indice_b PLS_INTEGER;
BEGIN
  -- Crear cuatro profesores. Se insertan uno por uno para que Oracle
  -- genere un ID distinto para cada identity.
  FOR p IN 1..4 LOOP
    INSERT INTO USUARIO (NOMBRE, CORREO, PASSWORD, ROL)
    VALUES (
      v_profesores(p),
      'profesor' || LPAD(p, 2, '0') || '@demo.edu',
      v_hash,
      'PROFESOR'
    ) RETURNING ID INTO v_ids_profesores(p);

    INSERT INTO ASIGNATURA (NOMBRE, CODIGO, PROFESOR_ID)
    VALUES (
      v_materias(p),
      'DEM' || TO_CHAR(100 + p),
      v_ids_profesores(p)
    ) RETURNING ID INTO v_ids_asignaturas(p);
  END LOOP;

  -- Diez nombres por diez apellidos producen cien estudiantes únicos.
  FOR i IN 1..100 LOOP
    v_correo := 'estudiante' || LPAD(i, 3, '0') || '@demo.edu';

    INSERT INTO USUARIO (NOMBRE, CORREO, PASSWORD, ROL)
    VALUES (
      v_nombres(TRUNC((i - 1) / 10) + 1) || ' ' ||
        v_apellidos(MOD(i - 1, 10) + 1),
      v_correo,
      v_hash,
      'ESTUDIANTE'
    ) RETURNING ID INTO v_id_estudiante;

    -- Cada estudiante se matricula en dos asignaturas diferentes.
    v_indice_a := MOD(i - 1, 4) + 1;
    v_indice_b := MOD(i, 4) + 1;

    INSERT INTO MATRICULA (ESTUDIANTE_ID, ASIGNATURA_ID)
    VALUES (v_id_estudiante, v_ids_asignaturas(v_indice_a))
    RETURNING ID INTO v_id_matricula;

    INSERT INTO CALIFICACION (MATRICULA_ID, EVALUACION, NOTA, FECHA)
    VALUES (v_id_matricula, 'Parcial 1', 2.50 + MOD(i, 25) / 10, SYSTIMESTAMP);
    INSERT INTO CALIFICACION (MATRICULA_ID, EVALUACION, NOTA, FECHA)
    VALUES (v_id_matricula, 'Trabajo final', 3.00 + MOD(i, 20) / 10, SYSTIMESTAMP);

    INSERT INTO MATRICULA (ESTUDIANTE_ID, ASIGNATURA_ID)
    VALUES (v_id_estudiante, v_ids_asignaturas(v_indice_b))
    RETURNING ID INTO v_id_matricula;

    INSERT INTO CALIFICACION (MATRICULA_ID, EVALUACION, NOTA, FECHA)
    VALUES (v_id_matricula, 'Parcial 1', 2.50 + MOD(i + 3, 25) / 10, SYSTIMESTAMP);
    INSERT INTO CALIFICACION (MATRICULA_ID, EVALUACION, NOTA, FECHA)
    VALUES (v_id_matricula, 'Trabajo final', 3.00 + MOD(i + 7, 20) / 10, SYSTIMESTAMP);
  END LOOP;
END;
/

COMMIT;

-- Verificar el cargue: 100 estudiantes, 4 profesores, 4 asignaturas,
-- 200 matriculas y 400 calificaciones.
SELECT ROL, COUNT(*) AS TOTAL
FROM USUARIO
GROUP BY ROL
ORDER BY ROL;

SELECT COUNT(*) AS TOTAL_ASIGNATURAS FROM ASIGNATURA;
SELECT COUNT(*) AS TOTAL_MATRICULAS FROM MATRICULA;
SELECT COUNT(*) AS TOTAL_CALIFICACIONES FROM CALIFICACION;
