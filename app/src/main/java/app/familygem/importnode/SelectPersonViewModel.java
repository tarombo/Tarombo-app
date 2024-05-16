package app.familygem.importnode;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SelectPersonViewModel extends ViewModel {
    public enum State{
        SELECT_PERSON_1,
        SELECT_RELATION,
        SELECT_PERSON_2,
        DONE
    }

    private final MutableLiveData<State> stateLiveData = new MutableLiveData<>(State.SELECT_PERSON_1);

    private String personId1 = null;
    private String personId2 = null;
    private String personName1 = null;
    private String personName2 = null;
    private int relationIndex = 0;
    private String familyId = null;
    private String placement = null;

    public void setPerson(String personId, String name){
        State state = this.stateLiveData.getValue();
        if(state == State.SELECT_PERSON_1 || state == State.SELECT_RELATION){
            personId1 = personId;
            personName1 = name;
            stateLiveData.postValue(State.SELECT_RELATION);
        }
        else if(state == State.SELECT_PERSON_2 || state == State.DONE){
            personId2 = personId;
            personName2 = name;
            stateLiveData.postValue(State.DONE);
        }
    }

    public void setState(State state){
        this.stateLiveData.setValue(state);
    }

    public LiveData<State> getState(){
        return stateLiveData;
    }

    public String getPersonId1(){
        return personId1;
    }

    public String getPersonId2(){
        return personId2;
    }

    public String getPersonName1(){
        return personName1;
    }

    public String getPersonName2(){
        return personName2;
    }

    public void setRelationIndex(int index){
        relationIndex = index;
    }

    public int getRelationIndex(){
        return relationIndex;
    }

    public void setFamilyId(String id){
        this.familyId = id;
    }

    public String getFamilyId(){
        return this.familyId;
    }

    public void setPlacement(String placement){
        this.placement = placement;
    }

    public String getPlacement(){
        return this.placement;
    }
}
