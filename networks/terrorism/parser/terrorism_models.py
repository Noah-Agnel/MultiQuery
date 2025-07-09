import hashlib
import json
from   dataclasses import dataclass
from   typing      import List


# NODES MODELS:

# 1. INCIDENT NODE:
@dataclass
class Incident:
    """
    Represents a terrorism incident with all its core attributes
    """
    def __init__(self, row):
        self.id            = row['eventid'        ]
        self.labels        =    ["Incident"       ]
        # DATE
        self.iyear         = row['iyear'          ]  if 'iyear'           in row else None
        self.imonth        = row['imonth'         ]  if 'imonth'          in row else None
        self.iday          = row['iday'           ]  if 'iday'            in row else None
        self.approxdate    = str(row['approxdate' ]) if 'approxdate'      in row else None
        self.resolution    = str(row['resolution' ]) if 'resolution'      in row else None
        self.extended      = row['extended'       ]  if 'extended'        in row else None
        # OTHERS ATTRIBUTES:
        self.attacks       =    [                 ]
        self.summary       = row['summary'        ]  if 'summary'         in row else None
        self.doubtterr     = row['doubtterr'      ]  if 'doubtterr'       in row else None
        self.multiple      = row['multiple'       ]  if 'multiple'        in row else None
        self.success       = row['success'        ]  if 'success'         in row else None
        self.suicide       = row['suicide'        ]  if 'suicide'         in row else None
        self.individual    = row['individual'     ]  if 'individual'      in row else None
        self.compclaim     = row['compclaim'      ]  if 'compclaim'       in row else None
        self.weapdetail    = row['weapdetail'     ]  if 'weapdetail'      in row else None
        self.dbsource      = row['dbsource'       ]  if 'dbsource'        in row else None
        self.addnotes      = row['addnotes'       ]  if 'addnotes'        in row else None
        self.alternative   = row['alternative_txt']  if 'alternative_txt' in row else None

    def set_attack_type(self, row:dict):
        """
        Sets the attack type of the incident
        """
        for attack_type in ['attacktype1', 'attacktype2', 'attacktype3']:
            if attack_type not in row:
                continue
            self.attacks.append(row[f'{attack_type}_txt'])

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id
    
    
# 2. DETAIL NODE:
@dataclass
class Detail:
    """
    Represents detailed statistics about an incident
    """
    def __init__(self, row):
        self.labels        =    ["Detail"         ]
        self.nperps        = row['nperps'         ] if 'nperps'          in row else None
        self.nperpcap      = row['nperpcap'       ] if 'nperpcap'        in row else None
        self.nkill         = row['nkill'          ] if 'nkill'           in row else None
        self.nkillus       = row['nkillus'        ] if 'nkillus'         in row else None
        self.nkillter      = row['nkillter'       ] if 'nkillter'        in row else None
        self.nwound        = row['nwound'         ] if 'nwound'          in row else None
        self.nwoundus      = row['nwoundus'       ] if 'nwoundus'        in row else None
        self.nwoundte      = row['nwoundte'       ] if 'nwoundte'        in row else None
        self.ishostkid     = row['ishostkid'      ] if 'ishostkid'       in row else None
        self.nhostkid      = row['nhostkid'       ] if 'nhostkid'        in row else None
        self.nhostkidus    = row['nhostkidus'     ] if 'nhostkidus'      in row else None
        # MAYBE EDGE PROPERTY
        self.nhours        = row['nhours'         ] if 'nhours'          in row else None
        self.ndays         = row['ndays'          ] if 'ndays'           in row else None
        self.divert        = row['divert'         ] if 'divert'          in row else None
        self.kidhijcountry = row['kidhijcountry'  ] if 'kidhijcountry'   in row else None
        self.nreleased     = row['nreleased'      ] if 'nreleased'       in row else None
        # ID COMPUTING
        self.id            = node_hashing_computing(self)
    
    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id

# 3. SOURCE NODE:
@dataclass
class Source:
    """
    Represents a source used to compile incident information
    maybe 1, 2, 3
    """
    def __init__(self, row, idx):
        self.labels        =    ["Source"         ]
        self.scite         = row[f'scite{idx}'    ] if f'scite{idx}'     in row else None
        self.id            = node_hashing_computing(self)
    
    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 4. CRITERIA NODE:
@dataclass
class Criteria:
    """
    Represents criteria for incident classification
    maybe 1, 2, 3
    """
    def __init__(self, row, idx):
        self.labels        =    ["Criteria"       ]
        self.crit          = row[f'crit{idx}'     ] if f'crit{idx}'      in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 5. MOTIVE NODE:
@dataclass
class Motive:
    """
    Represents the motive for an attack
    """
    def __init__(self, row):
        self.labels        =    ["Motive"         ]
        self.motive        = row['motive'         ] if 'motive'          in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 6. COUNTRY NODE:
@dataclass
class Country:
    """
    Represents a country
    """
    def __init__(self, row):
        self.labels        =    ["Country"        ]
        self.id            = row['country'        ] if 'country'         in row else None
        self.country       = row['country_txt'    ] if 'country_txt'     in row else None
    
    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id

# 7. REGION NODE:
@dataclass
class Region:
    """
    Represents a geographical region
    """
    def __init__(self, row):
        self.labels        =    ["Region"         ]
        self.id            = row['region'         ] if 'region'          in row else None
        self.region        = row['region_txt'     ] if 'region_txt'      in row else None

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 8. PROVINCE ADMINISTRATIVE STATE NODE:
@dataclass
class ProvinceAdministrativeState:
    """
    Represents a province, administrative region, or state
    """
    def __init__(self, row):
        self.labels        =    ["Province"       ]
        self.provstate     = row['provstate'      ] if 'provstate'       in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 9. CITY NODE:
@dataclass
class City:
    """
    Represents a city with geographical coordinates
    """
    def __init__(self, row):
        self.labels        =    ["City"          ]
        self.city          = row['city'          ] if 'city'             in row else None
        self.latitude      = row['latitude'      ] if 'latitude'         in row else None
        self.longitude     = row['longitude'     ] if 'longitude'        in row else None
        self.specificity   = row['specificity'   ] if 'specificity'      in row else None
        self.vicinity      = row['vicinity'      ] if 'vicinity'         in row else None
        self.location      = row['location'      ] if 'location'         in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 10. PROPERTY NODE:
@dataclass
class Property:
    """
    Represents property damage information
    """
    def __init__(self, row):
        self.labels        =    ["Property"      ]
        self.property      = row['property'      ] if 'property'         in row else None
        self.propextentid  = row['propextent'    ] if 'propextent'       in row else None
        self.propextent    = row['propextent_txt'] if 'propextent_txt'   in row else None
        self.propvalue     = row['propvalue'     ] if 'propvalue'        in row else None
        self.propcomment   = row['propcomment'   ] if 'propcomment'      in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 11. WEAPON NODE:
@dataclass
class Weapon:
    """
    Represents weapon information
    maybe from one to four weapons
    """
    def __init__(self, row, idx):   
        self.labels        =    ["Weapon"               ]
        self.weaptypeid    = row[f'weaptype{idx}'       ] if f'weaptype{idx}'        in row else None
        self.weaptype      = row[f'weaptype{idx}_txt'   ] if f'weaptype{idx}_txt'    in row else None
        self.weapsubtypeid = row[f'weapsubtype{idx}'    ] if f'weapsubtype{idx}'     in row else None
        self.weapsubtype   = row[f'weapsubtype{idx}_txt'] if f'weapsubtype{idx}_txt' in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 12. CLAIM NODE:
@dataclass
class Claim:
    """
    Represents responsibility claims
    maybe ed, 2, 3
    """
    def __init__(self, row, idx:str):
        nidx = idx if idx != 'ed' else ''
        self.labels        =    ["Claim"                 ]
        self.claim         = row[f'claim{idx}'           ] if f'claim{idx}'           in row else None
        self.claimmodeid   = row[f'claimmode{nidx}'      ] if f'claimmode{nidx}'      in row else None
        self.claimmode     = row[f'claimmode{nidx}_txt'  ] if f'claimmode{nidx}_txt'  in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 13. GROUP NAME NODE:
@dataclass  
class GroupName:
    """
    Represents the group that carried out the attack
    maybe '', 2, 3
    """
    def __init__(self, row, idx):
        nidx = idx if idx != '' else 1
        self.labels        =    ["GroupName"         ]
        self.gname         = row[f'gname{idx}'       ] if f'gname{idx}'               in row else None
        self.gsubname      = row[f'gsubname{idx}'    ] if f'gsubname{idx}'            in row else None
        self.guncertain    = row[f'guncertain{nidx}' ] if f'guncertain{nidx}'         in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 14. RANSOM NODE:
@dataclass
class Ransom:
    """
    Represents ransom demand information
    """
    def __init__(self, row):
        self.labels        =    ["Ransom"                 ]
        self.ransom        = row['ransom'        ] if 'ransom'                        in row else None
        self.ransomamt     = row['ransomamt'     ] if 'ransomamt'                     in row else None
        self.ransomamtus   = row['ransomamtus'   ] if 'ransomamtus'                   in row else None
        self.ransompaid    = row['ransompaid'    ] if 'ransompaid'                    in row else None
        self.ransompaidus  = row['ransompaidus'  ] if 'ransompaidus'                  in row else None
        self.ransomnote    = row['ransomnote'    ] if 'ransomnote'                    in row else None
        self.id            = node_hashing_computing(self)

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 15. HOSTAGE OUTCOME NODE:
@dataclass
class HostageOutcome:
    """
    Represents the eventual fate of hostages and kidnap victims
    """
    def __init__(self, row):
        self.labels           =    ["HostageOutcome"     ]
        self.id               = row['hostkidoutcome'     ] if 'hostkidoutcome'       in row else None
        self.hostkidoutcome   = row['hostkidoutcome_text'] if 'hostkidoutcome_text'  in row else None

    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# 16. TARGET NODE:
@dataclass
class Target:
    """
    Represents the target of an attack
    maybe 1, 2, 3
    """
    def __init__(self, row, idx):
        self.labels          =    ["Target"               ]
        self.corp            = row[f'corp{idx}'           ] if f'corp{idx}'           in row else None
        self.target          = row[f'target{idx}'         ] if f'target{idx}'         in row else None
        self.targtype_id     = row[f'targtype{idx}'       ] if f'targtype{idx}'       in row else None
        self.targtype        = row[f'targtype{idx}_txt'   ] if f'targtype{idx}_txt'   in row else None
        self.targsubtype_id  = row[f'targsubtype{idx}'    ] if f'targsubtype{idx}'    in row else None
        self.targsubtype     = row[f'targsubtype{idx}_txt'] if f'targsubtype{idx}_txt' in row else None
        self.id              = node_hashing_computing(self)
        
    def get_id(self):
        """
        Returns the id of the node
        """
        return self.id


# NODES MANAGING:
@dataclass
class NodesManaging:
    """
    Represents the nodes managing
    """
    def __init__(self):
        self.nodes      = {}
        self.code_nodes = {
            'incident'          : set(),
            'detail'            : set(),
            'source'            : set(),
            'criteria'          : set(),
            'motive'            : set(),
            'country'           : set(),
            'region'            : set(),
            'prov_adminis_state': set(),
            'city'              : set(),
            'property'          : set(),
            'weapon'            : set(),
            'claim'             : set(),
            'group_name'        : set(),
            'ransom'            : set(),
            'hostage_outcome'   : set(),
            'target'            : set()
        }
        self.reset_nodes()

    def reset_nodes(self):
        """
        Resets the nodes
        """
        self.nodes = {
            'incident'          : [],
            'detail'            : [],
            'source'            : [],
            'criteria'          : [],
            'motive'            : [],
            'country'           : [],
            'region'            : [],
            'prov_adminis_state': [],
            'city'              : [],
            'property'          : [],
            'weapon'            : [],
            'claim'             : [],
            'group_name'        : [],
            'ransom'            : [],
            'hostage_outcome'   : [],
            'target'            : []
        }
    
    def add_node(self, node, node_type):
        """
        Adds a node to the nodes
        """
        node_id = node.get_id()
        if node_id is None:
            return None
        
        if node_id in self.code_nodes[node_type]:
            return node_id
        
        self.code_nodes[node_type].add(node_id)
        self.nodes[node_type].append(node)
        return node_id
    

@dataclass
class EdgesManaging:
    """
    Represents the edges managing
    """
    def __init__(self):
        self.edges      = []
        self.code_edges = set()

    def add_edge(self, src_id, dst_id, edge_type):
        """
        Adds an edge to the edges
        """
        if src_id is None or dst_id is None:
            return
        
        edge = {
            'src'  : src_id,
            'dst'  : dst_id,
            'type' : edge_type
        }
        
        edge_hash = hashlib.sha256(json.dumps(edge, sort_keys=True).encode('utf-8')).hexdigest()
        
        if edge_hash in self.code_edges:
            return

        edge['edge_id'] = edge_hash
        self.edges.append(edge)
        self.code_edges.add(edge_hash)
    
    def reset_edges(self):
        """
        Resets the edges
        """
        self.edges = [] 


# FUNCTIONS:
# 1. NODE HASHING COMPUTING:
def node_hashing_computing(node):
    """
    Computes the hash of a node
    """
    node_dict = node.__dict__
    if all(value is None for value in node_dict.values()):
        return None

    hash_value = hashlib.sha256(
        json.dumps(node_dict, sort_keys=True)   
            .encode('utf-8')
        ).hexdigest()
    return hash_value


# 2. NODES AND EDGES CREATION:
def nodes_edges_creation(row, node_manager:NodesManaging, edge_manager:EdgesManaging):
    """
    Creates nodes and edges from a row
    """
    # NODES CREATION:
    incident_id          = node_manager.add_node(Incident(row)      , 'incident'  )
    if incident_id is None:
        return
    
    detail_id             = node_manager.add_node(Detail(row)        , 'detail'    )
    first_source_id       = node_manager.add_node(Source(row, 1)     , 'source'    )
    second_source_id      = node_manager.add_node(Source(row, 2)     , 'source'    )
    third_source_id       = node_manager.add_node(Source(row, 3)     , 'source'    )
    first_criteria_id     = node_manager.add_node(Criteria(row, 1)   , 'criteria'  )
    second_criteria_id    = node_manager.add_node(Criteria(row, 2)   , 'criteria'  )
    third_criteria_id     = node_manager.add_node(Criteria(row, 3)   , 'criteria'  )
    motive_id             = node_manager.add_node(Motive(row)        , 'motive'    )
    country_id            = node_manager.add_node(Country(row)       , 'country'   )
    region_id             = node_manager.add_node(Region(row)        , 'region'    )  
    prov_adm_state_id     = node_manager.add_node(ProvinceAdministrativeState(row), 'prov_adminis_state')
    city_id               = node_manager.add_node(City(row)          , 'city'      )
    property_id           = node_manager.add_node(Property(row)      , 'property'  )
    first_weapon_id       = node_manager.add_node(Weapon(row, 1)     , 'weapon'    )
    second_weapon_id      = node_manager.add_node(Weapon(row, 2)     , 'weapon'    )
    third_weapon_id       = node_manager.add_node(Weapon(row, 3)     , 'weapon'    )
    fourth_weapon_id      = node_manager.add_node(Weapon(row, 4)     , 'weapon'    )
    first_claim_id        = node_manager.add_node(Claim(row, 'ed')   , 'claim'     )
    second_claim_id       = node_manager.add_node(Claim(row, 2)      , 'claim'     )
    third_claim_id        = node_manager.add_node(Claim(row, 3)      , 'claim'     )
    first_group_name_id   = node_manager.add_node(GroupName(row, '') , 'group_name')
    second_group_name_id  = node_manager.add_node(GroupName(row, 2)  , 'group_name')
    third_group_name_id   = node_manager.add_node(GroupName(row, 3)  , 'group_name')
    ransom_id             = node_manager.add_node(Ransom(row)        , 'ransom'    )
    hostage_outcome_id    = node_manager.add_node(HostageOutcome(row), 'hostage_outcome')
    first_target_id       = node_manager.add_node(Target(row, 1)     , 'target'    )
    second_target_id      = node_manager.add_node(Target(row, 2)     , 'target'    )
    third_target_id       = node_manager.add_node(Target(row, 3)     , 'target'    )
    first_nationality_id  = node_manager.add_node(Country(row, 1)    , 'country'   )
    second_nationality_id = node_manager.add_node(Country(row, 2)    , 'country'   )
    third_nationality_id  = node_manager.add_node(Country(row, 3)    , 'country'   )
    relateds              = row['related'].split(",") if 'related' in row else []

    # EDGES CREATION:
    # DETAIL:
    if detail_id is not None:
        edge_manager.add_edge(incident_id, detail_id            , 'IS_CHARACTERIZED_BY')
    
    # SOURCES:
    if first_source_id is not None:
        edge_manager.add_edge(incident_id, first_source_id      , 'IS_POWERED_BY')
    if second_source_id is not None:
        edge_manager.add_edge(incident_id, second_source_id     , 'IS_POWERED_BY')
    if third_source_id is not None:
        edge_manager.add_edge(incident_id, third_source_id      , 'IS_POWERED_BY')

    # CRITERIA:
    if first_criteria_id is not None:
        edge_manager.add_edge(incident_id, first_criteria_id    , 'IS_BASED_ON')
    if second_criteria_id is not None:
        edge_manager.add_edge(incident_id, second_criteria_id   , 'IS_BASED_ON')
    if third_criteria_id is not None:
        edge_manager.add_edge(incident_id, third_criteria_id    , 'IS_BASED_ON')

    # MOTIVE:
    if motive_id is not None:
        edge_manager.add_edge(incident_id, motive_id            , 'IS_CAUSED_BY')

    # COUNTRY:
    if country_id is not None:
        edge_manager.add_edge(incident_id, country_id           , 'OCCURRED_IN')
    if region_id is not None:
        edge_manager.add_edge(incident_id, region_id            , 'OCCURRED_IN')
    if prov_adm_state_id is not None:
        edge_manager.add_edge(incident_id, prov_adm_state_id    , 'OCCURRED_IN')
    if city_id is not None:
        edge_manager.add_edge(incident_id, city_id              , 'OCCURRED_IN')

    # PROPERTY:
    if property_id is not None:
        edge_manager.add_edge(incident_id, property_id          , 'HAS_PROPERTY')

    # WEAPON:
    if first_weapon_id is not None:
        edge_manager.add_edge(incident_id, first_weapon_id      , 'EMPLOYED')
    if second_weapon_id is not None:
        edge_manager.add_edge(incident_id, second_weapon_id     , 'EMPLOYED')
    if third_weapon_id is not None:
        edge_manager.add_edge(incident_id, third_weapon_id      , 'EMPLOYED')
    if fourth_weapon_id is not None:
        edge_manager.add_edge(incident_id, fourth_weapon_id     , 'EMPLOYED')

    # CLAIM:
    if first_claim_id is not None:
        edge_manager.add_edge(incident_id, first_claim_id       , 'HAS_CLAIM')
    if second_claim_id is not None:
        edge_manager.add_edge(incident_id, second_claim_id      , 'HAS_CLAIM')
    if third_claim_id is not None:
        edge_manager.add_edge(incident_id, third_claim_id       , 'HAS_CLAIM')

    # GROUP NAME:
    if first_group_name_id is not None:
        edge_manager.add_edge(incident_id, first_group_name_id  , 'IS_CARRIED_OUT_BY')
    if second_group_name_id is not None:
        edge_manager.add_edge(incident_id, second_group_name_id , 'IS_CARRIED_OUT_BY')
    if third_group_name_id is not None:
        edge_manager.add_edge(incident_id, third_group_name_id  , 'IS_CARRIED_OUT_BY')

    # RANSOM:
    if ransom_id is not None:
        edge_manager.add_edge(incident_id, ransom_id            , 'DEMANDS')

    # HOSTAGE OUTCOME:
    if hostage_outcome_id is not None:
        edge_manager.add_edge(incident_id, hostage_outcome_id   , 'HAS')

    # TARGET:
    if first_target_id is not None:
        edge_manager.add_edge(incident_id, first_target_id      , 'IS_MEANT_TO')
        if first_nationality_id is not None:
            edge_manager.add_edge(
                first_target_id,
                first_nationality_id, 
                'HAS_NATIONALITY'
            )
    if second_target_id is not None:
        edge_manager.add_edge(incident_id, second_target_id     , 'IS_MEANT_TO')
        if second_nationality_id is not None:
            edge_manager.add_edge(
                second_target_id, 
                second_nationality_id, 
                'HAS_NATIONALITY'
            )
    if third_target_id is not None:
        edge_manager.add_edge(incident_id, third_target_id      , 'IS_MEANT_TO')
        if third_nationality_id is not None:
            edge_manager.add_edge(
                third_target_id, 
                third_nationality_id, 
                'HAS_NATIONALITY'
            )

    # RELATEDS:
    for related in relateds:
        if related is not None:
            edge_manager.add_edge(incident_id, related          , 'IS_RELATED_TO')