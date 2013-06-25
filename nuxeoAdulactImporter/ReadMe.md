

## MVEL Context 

 - `currentDocument` : last created DocumentModel
 - `currentElement` : last parsed DOM4J tag Element
 - `xml` : XML input document as parsed by DOM4J
 - `map` : Mapping between DOM4J ELements and associated created DocumentModel (Element object is the key)
 - `rootDoc` : root DocumentModel where the import was started
 - `docs` : list of imported DocumentModels
 - `session` : CoreSession

## TODO

 - add MVEL functions :
    - slice function / mkdir
    - add date formatting / DateWrapper++
 - manage real Blobs
 



