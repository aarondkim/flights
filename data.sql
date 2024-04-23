CREATE EXTERNAL DATA SOURCE flightdata_blob
WITH (TYPE = BLOB_STORAGE, 
LOCATION = 'https://introdatamanagement.blob.core.windows.net/flightdata'
);